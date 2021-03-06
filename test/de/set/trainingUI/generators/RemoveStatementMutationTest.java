package de.set.trainingUI.generators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

public class RemoveStatementMutationTest {

    private static CompilationUnit parse(final String fileContent) {
        return StaticJavaParser.parse(fileContent);
    }

    static<T extends Mutation> List<T> determineApplicableMutations(
    		String code, Class<T> mutationType) {
    	return determineApplicableMutations(parse(code), mutationType);
    }
    private static<T extends Mutation> List<T> determineApplicableMutations(
    		CompilationUnit cu, Class<T> mutationType) {
    	final List<Mutation> mutations = MutationGenerator.findPossibleMutations(cu);
    	mutations.removeIf((Mutation m) -> !mutationType.isInstance(m));
    	return (List<T>) mutations;
    }

    static void checkMutation(
    		String input,
    		Class<? extends Mutation> mutationType,
    		int mutationIndex,
    		String expectedSource) {
    	checkMutation(input, mutationType, mutationIndex, 123L, expectedSource);
    }

    static void checkMutation(
    		String input,
    		Class<? extends Mutation> mutationType,
    		int mutationIndex,
    		long seed,
    		String expectedSource) {
    	final CompilationUnit cu = parse(input);
        final List<? extends Mutation> m = determineApplicableMutations(cu, mutationType);
    	m.get(mutationIndex).apply(new Random(seed));

    	assertEquals(expectedSource, cu.toString());

        // also smoke test that creating the remarks will not fail
        m.get(mutationIndex).createRemark(1, new RemarkCreator(new Properties(), LineMap.identity()));
    }

    static void checkMutations(
    		String input,
    		Class<? extends Mutation> mutationType,
    		int mutationIndex,
    		String... expectedSources) {
    	final Set<String> allPossibilities = new LinkedHashSet<>();
    	for (int seed = -10; seed < 1000; seed++) {
	    	final CompilationUnit cu = parse(input);
	        final List<? extends Mutation> m = determineApplicableMutations(cu, mutationType);
	    	m.get(mutationIndex).apply(new Random(seed));
	    	allPossibilities.add(cu.toString());
    	}

    	assertThat(allPossibilities, containsInAnyOrder(expectedSources));
    }

    static void checkMutationCount(
    		String input,
    		Class<? extends Mutation> mutationType,
    		int expectedCount) {
    	final CompilationUnit cu = parse(input);
        final List<? extends Mutation> m = determineApplicableMutations(cu, mutationType);
        assertEquals(expectedCount, m.size());
    }

    @Test
    public void testSingleStatementsAreNotRemoved() {
        final String input =
                "class A {\n"
                + "    public void a() {\n"
                + "        System.out.println(\"x\");\n"
                + "    }\n"
                + "}\n";

        checkMutationCount(input, RemoveStatementMutation.class, 0);
    }

    @Test
    public void testRemoveMethodCall() {
        final String input =
                "class A {\n"
                + "    public void a() {\n"
                + "        System.out.println(\"x\");\n"
                + "        System.out.println(\"y\");\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		RemoveStatementMutation.class,
        		0,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        System.out.println(\"y\");\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		RemoveStatementMutation.class,
        		1,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        System.out.println(\"x\");\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, RemoveStatementMutation.class, 2);
    }

    @Test
    public void testRemoveChainedCall() {
        final String input =
                "class A {\n"
                + "    public void a() {\n"
                + "        String asdf = \"qwer\";\n"
                + "        Runtime.getRuntime().doStuff(asdf);\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		RemoveStatementMutation.class,
        		0,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        String asdf = \"qwer\";\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, RemoveStatementMutation.class, 1);
    }

    @Test
    public void testRemoveCompleteIfStatement() {
        final String input =
                "class A {\n"
                + "    public void a() {\n"
                + "        System.out.println(\"x\");\n"
                + "        if (System.err != null) {\n"
                + "            System.out.println(\"y\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		RemoveStatementMutation.class,
        		0,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        if (System.err != null) {\n"
                + "            System.out.println(\"y\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		RemoveStatementMutation.class,
        		1,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        System.out.println(\"x\");\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, RemoveStatementMutation.class, 2);
    }

    @Test
    public void testRemoveStatementInIf() {
        final String input =
                "class A {\n"
                + "    public void a() {\n"
                + "        if (System.err != null) {\n"
                + "            System.out.println(\"x\");\n"
                + "            System.out.println(\"y\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		RemoveStatementMutation.class,
        		0,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        if (System.err != null) {\n"
                + "            System.out.println(\"y\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		RemoveStatementMutation.class,
        		1,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        if (System.err != null) {\n"
                + "            System.out.println(\"x\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, RemoveStatementMutation.class, 2);
    }

    @Test
    public void testRemoveContinue() {
        final String input =
                "class A {\n"
                + "    public void a() {\n"
                + "        while (System.err != null) {\n"
                + "            if (System.out == null) {\n"
                + "                System.err.println(\"y\");\n"
                + "                continue;\n"
                + "            }\n"
                + "            System.out.println(\"y\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		RemoveStatementMutation.class,
        		0,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        while (System.err != null) {\n"
                + "            if (System.out == null) {\n"
                + "                continue;\n"
                + "            }\n"
                + "            System.out.println(\"y\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		RemoveStatementMutation.class,
        		1,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        while (System.err != null) {\n"
                + "            if (System.out == null) {\n"
                + "                System.err.println(\"y\");\n"
                + "            }\n"
                + "            System.out.println(\"y\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		RemoveStatementMutation.class,
        		2,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        while (System.err != null) {\n"
                + "            System.out.println(\"y\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		RemoveStatementMutation.class,
        		3,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        while (System.err != null) {\n"
                + "            if (System.out == null) {\n"
                + "                System.err.println(\"y\");\n"
                + "                continue;\n"
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, RemoveStatementMutation.class, 4);
    }

    @Test
    public void testRemoveBreak() {
        final String input =
                "class A {\n"
                + "    public void a() {\n"
                + "        while (System.err != null) {\n"
                + "            if (System.out == null) {\n"
                + "                System.err.println(\"y\");\n"
                + "                break;\n"
                + "            }\n"
                + "            System.out.println(\"y\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		RemoveStatementMutation.class,
        		0,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        while (System.err != null) {\n"
                + "            if (System.out == null) {\n"
                + "                break;\n"
                + "            }\n"
                + "            System.out.println(\"y\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		RemoveStatementMutation.class,
        		1,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        while (System.err != null) {\n"
                + "            if (System.out == null) {\n"
                + "                System.err.println(\"y\");\n"
                + "            }\n"
                + "            System.out.println(\"y\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		RemoveStatementMutation.class,
        		2,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        while (System.err != null) {\n"
                + "            System.out.println(\"y\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		RemoveStatementMutation.class,
        		3,
                "class A {\n"
                + "\n"
                + "    public void a() {\n"
                + "        while (System.err != null) {\n"
                + "            if (System.out == null) {\n"
                + "                System.err.println(\"y\");\n"
                + "                break;\n"
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, RemoveStatementMutation.class, 4);
    }

    @Test
    public void testLambda() {
    	final String input =
	        "class A {\n"
	    	+ "	 public static<T> PriorityQueue<?> foo() {\n"
	    	+ "		PriorityQueue<?> ret = new PriorityQueue<List<Predicate<T>>>(\n"
	    	+ "				(List<Predicate<T>> l1, List<Predicate<T>> l2) -> Integer.compare(l2.size(), l1.size()));\n"
	    	+ "		return ret;\n"
	    	+ "    }\n"
	    	+ "}\n";
        checkMutationCount(input, RemoveStatementMutation.class, 0);
    }

    @Test
    public void testRemoveVoidReturn() {
        final String input =
                "class A {\n"
                + "    public void a() {\n"
                + "        if (x) {\n"
                + "            doStuff();\n"
                + "            return;\n"
                + "        }\n"
                + "        doOtherStuff();\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		RemoveStatementMutation.class,
        		0,
                "class A {\n"
        		+ "\n"
                + "    public void a() {\n"
                + "        if (x) {\n"
                + "            return;\n"
                + "        }\n"
                + "        doOtherStuff();\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		RemoveStatementMutation.class,
        		1,
                "class A {\n"
        		+ "\n"
                + "    public void a() {\n"
                + "        if (x) {\n"
                + "            doStuff();\n"
                + "        }\n"
                + "        doOtherStuff();\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		RemoveStatementMutation.class,
        		2,
                "class A {\n"
        		+ "\n"
                + "    public void a() {\n"
                + "        doOtherStuff();\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		RemoveStatementMutation.class,
        		3,
                "class A {\n"
        		+ "\n"
                + "    public void a() {\n"
                + "        if (x) {\n"
                + "            doStuff();\n"
                + "            return;\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, RemoveStatementMutation.class, 4);
    }

    @Test
    public void testRemoveValuedReturn() {
        final String input =
                "class A {\n"
                + "    public int a() {\n"
                + "        if (x) {\n"
                + "            doStuff();\n"
                + "            return 47;\n"
                + "        }\n"
                + "        return 5;\n"
                + "    }\n"
                + "}\n";

        checkMutation(input,
        		RemoveStatementMutation.class,
        		0,
                "class A {\n"
        		+ "\n"
                + "    public int a() {\n"
                + "        if (x) {\n"
                + "            return 47;\n"
                + "        }\n"
                + "        return 5;\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		RemoveStatementMutation.class,
        		1,
                "class A {\n"
        		+ "\n"
                + "    public int a() {\n"
                + "        if (x) {\n"
                + "            doStuff();\n"
                + "        }\n"
                + "        return 5;\n"
                + "    }\n"
                + "}\n");
        checkMutation(input,
        		RemoveStatementMutation.class,
        		2,
                "class A {\n"
        		+ "\n"
                + "    public int a() {\n"
                + "        return 5;\n"
                + "    }\n"
                + "}\n");
        checkMutationCount(input, RemoveStatementMutation.class, 3);
    }

}
