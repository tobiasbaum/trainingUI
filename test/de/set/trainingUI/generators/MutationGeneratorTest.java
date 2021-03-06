package de.set.trainingUI.generators;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import org.junit.Test;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.Statement;

import de.set.trainingUI.RemarkType;

public class MutationGeneratorTest {

	private static final String TESTCODE =
            "class A {\n"
            + "\n"
            + "    public int a() {\n"
            + "        String a1 = \"test1\";\n"
            + "        String a2 = \"test2\";\n"
            + "        String a3 = \"test3\";\n"
            + "        return 42;\n"
            + "    }\n"
            + "}\n";

	private static class ChangeTextMutation extends Mutation {

		private final StringLiteralExpr expr;

		public ChangeTextMutation(StringLiteralExpr expr) {
			this.expr = expr;
		}

		@Override
		public void apply(Random r) {
			this.expr.setValue(this.expr.getValue() + "changed");
		}

		@Override
		public void createRemark(int nbr, RemarkCreator taskProperties) {
			taskProperties.setRemark(nbr, this.getAnchorLine(), Collections.singleton(this.getAnchorLine()),
					RemarkType.OTHER_ALGORITHMIC_PROBLEM, "...", "changed");
		}

		@Override
		public int getAnchorLine() {
			return this.expr.getBegin().get().line;
		}

	}

	private static class DeleteTextMutation extends Mutation {

		private final StringLiteralExpr expr;

		public DeleteTextMutation(StringLiteralExpr expr) {
			this.expr = expr;
		}

		@Override
		public void apply(Random r) {
			Node cur = this.expr;
			while (!(cur instanceof Statement)) {
				cur = cur.getParentNode().get();
			}
			cur.remove();
		}

		@Override
		public void createRemark(int nbr, RemarkCreator taskProperties) {
			taskProperties.setRemark(nbr, this.getAnchorLine(), Collections.singleton(this.getAnchorLine()),
					RemarkType.OTHER_ALGORITHMIC_PROBLEM, "...", "deleted");
		}

		@Override
		public int getAnchorLine() {
			return this.expr.getBegin().get().line;
		}

	}

	private static class NoOpMutation extends Mutation {

		public NoOpMutation() {
		}

		@Override
		public void apply(Random r) {
		}

		@Override
		public void createRemark(int nbr, RemarkCreator taskProperties) {
		}

		@Override
		public int getAnchorLine() {
			return 1;
		}

	}

	private static Function<CompilationUnit, List<Mutation>> change(String... strings) {
		final Set<String> toChange = toSet(strings);
		return (CompilationUnit ast) -> {
			final List<Mutation> ret = new ArrayList<>();
			for (final StringLiteralExpr expr : ast.findAll(
					StringLiteralExpr.class,
					(StringLiteralExpr lit) -> toChange.contains(lit.getValue()))) {
				ret.add(new ChangeTextMutation(expr));
			}
			return ret;
		};
	}

	private static Function<CompilationUnit, List<Mutation>> delete(String... strings) {
		final Set<String> toChange = toSet(strings);
		return (CompilationUnit ast) -> {
			final List<Mutation> ret = new ArrayList<>();
			for (final StringLiteralExpr expr : ast.findAll(
					StringLiteralExpr.class,
					(StringLiteralExpr lit) -> toChange.contains(lit.getValue()))) {
				ret.add(new DeleteTextMutation(expr));
			}
			return ret;
		};
	}

	private static Function<CompilationUnit, List<Mutation>> deleteOrChange(String... strings) {
		final Set<String> toChange = toSet(strings);
		return (CompilationUnit ast) -> {
			final List<Mutation> ret = new ArrayList<>();
			for (final StringLiteralExpr expr : ast.findAll(
					StringLiteralExpr.class,
					(StringLiteralExpr lit) -> toChange.contains(lit.getValue()))) {
				ret.add(new ChangeTextMutation(expr));
				ret.add(new DeleteTextMutation(expr));
			}
			return ret;
		};
	}

	private static final class DeleteOptimization implements CodeOptimization {

		private final String marker;

		public DeleteOptimization(String marker) {
			this.marker = marker;
		}

		@Override
		public boolean optimize(CompilationUnit cu) {
			final List<Mutation> deletions = delete(this.marker).apply(cu);
			for (final Mutation d : deletions) {
				d.apply(new Random(123));
			}
			return !deletions.isEmpty();
		}

	}


	private static Set<String> toSet(String... strings) {
		final Set<String> toChange = new HashSet<>(Arrays.asList(strings));
		return toChange;
	}


    @Test
    public void testExceptionWhenNoMutations() {
    	final Properties taskProperties = new Properties();
    	try {
	    	MutationGenerator.applyMutations(
	    			taskProperties,
	    			new Random(123),
	    			1,
	    			TESTCODE,
	    			(ast) -> Collections.emptyList(),
	    			CompositeOptimization.empty());
    	} catch (final IllegalArgumentException e) {
    		assertThat(e.getMessage(), startsWith("no mutations found"));
    	}
    }

    @Test
    public void testExceptionWhenMutationDoesNotChange() {
    	final Properties taskProperties = new Properties();
    	try {
	    	MutationGenerator.applyMutations(
	    			taskProperties,
	    			new Random(123),
	    			1,
	    			TESTCODE,
	    			(ast) -> Collections.singletonList(new NoOpMutation()),
	    			CompositeOptimization.empty());
    	} catch (final IllegalArgumentException e) {
    		assertThat(e.getMessage(), startsWith("mutation did not change anything"));
    	}
    }

    @Test
    public void testSingleMutation() {
    	final Properties taskProperties = new Properties();
    	final String result = MutationGenerator.applyMutations(
    			taskProperties,
    			new Random(123),
    			1,
    			TESTCODE,
    			change("test1"),
    			CompositeOptimization.empty());
    	assertEquals(
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        String a1 = \"test1changed\";\n"
                + "        String a2 = \"test2\";\n"
                + "        String a3 = \"test3\";\n"
                + "        return 42;\n"
                + "    }\n"
                + "}\n",
    			result);
    	assertEquals("4;OTHER_ALGORITHMIC_PROBLEM;changed", taskProperties.getProperty("remark.1.example"));
    }

    @Test
    public void testSingleMutationWhenNotMorePossible() {
    	final Properties taskProperties = new Properties();
    	final String result = MutationGenerator.applyMutations(
    			taskProperties,
    			new Random(123),
    			2,
    			TESTCODE,
    			change("test1"),
    			CompositeOptimization.empty());
    	assertEquals(
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        String a1 = \"test1changed\";\n"
                + "        String a2 = \"test2\";\n"
                + "        String a3 = \"test3\";\n"
                + "        return 42;\n"
                + "    }\n"
                + "}\n",
    			result);
    	assertEquals("4;OTHER_ALGORITHMIC_PROBLEM;changed", taskProperties.getProperty("remark.1.example"));
    }

    @Test
    public void testTwoMutations() {
    	final Properties taskProperties = new Properties();
    	final String result = MutationGenerator.applyMutations(
    			taskProperties,
    			new Random(123),
    			2,
    			TESTCODE,
    			change("test1", "test3"),
    			CompositeOptimization.empty());
    	assertEquals(
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        String a1 = \"test1changed\";\n"
                + "        String a2 = \"test2\";\n"
                + "        String a3 = \"test3changed\";\n"
                + "        return 42;\n"
                + "    }\n"
                + "}\n",
    			result);

    	assertEquals(
    			toSet("4;OTHER_ALGORITHMIC_PROBLEM;changed",
    					"6;OTHER_ALGORITHMIC_PROBLEM;changed"),
    			toSet(taskProperties.getProperty("remark.1.example"),
    					taskProperties.getProperty("remark.2.example")));
    }

    @Test
    public void testThreeMutations() {
    	final Properties taskProperties = new Properties();
    	final String result = MutationGenerator.applyMutations(
    			taskProperties,
    			new Random(123),
    			3,
    			TESTCODE,
    			change("test1", "test2", "test3"),
    			CompositeOptimization.empty());
    	assertEquals(
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        String a1 = \"test1changed\";\n"
                + "        String a2 = \"test2changed\";\n"
                + "        String a3 = \"test3changed\";\n"
                + "        return 42;\n"
                + "    }\n"
                + "}\n",
    			result);

    	assertEquals(
    			toSet("4;OTHER_ALGORITHMIC_PROBLEM;changed",
    					"5;OTHER_ALGORITHMIC_PROBLEM;changed",
    					"6;OTHER_ALGORITHMIC_PROBLEM;changed"),
    			toSet(taskProperties.getProperty("remark.1.example"),
    					taskProperties.getProperty("remark.2.example"),
    					taskProperties.getProperty("remark.3.example")));
    }

    @Test
    public void testRandomChoiceWhenTwoPossibleButOnlyOneNeeded1() {
    	final Properties taskProperties = new Properties();
    	final String result = MutationGenerator.applyMutations(
    			taskProperties,
    			new Random(123),
    			1,
    			TESTCODE,
    			change("test1", "test3"),
    			CompositeOptimization.empty());
    	assertEquals(
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        String a1 = \"test1\";\n"
                + "        String a2 = \"test2\";\n"
                + "        String a3 = \"test3changed\";\n"
                + "        return 42;\n"
                + "    }\n"
                + "}\n",
    			result);
    }

    @Test
    public void testRandomChoiceWhenTwoPossibleButOnlyOneNeeded2() {
    	final Properties taskProperties = new Properties();
    	final String result = MutationGenerator.applyMutations(
    			taskProperties,
    			new Random(-1),
    			1,
    			TESTCODE,
    			change("test1", "test3"),
    			CompositeOptimization.empty());
    	assertEquals(
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        String a1 = \"test1changed\";\n"
                + "        String a2 = \"test2\";\n"
                + "        String a3 = \"test3\";\n"
                + "        return 42;\n"
                + "    }\n"
                + "}\n",
    			result);
    }

    @Test
    public void testSingleDeletion() {
    	final Properties taskProperties = new Properties();
    	final String result = MutationGenerator.applyMutations(
    			taskProperties,
    			new Random(123),
    			1,
    			TESTCODE,
    			delete("test1"),
    			CompositeOptimization.empty());
    	assertEquals(
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        String a2 = \"test2\";\n"
                + "        String a3 = \"test3\";\n"
                + "        return 42;\n"
                + "    }\n"
                + "}\n",
    			result);
    	assertEquals("4;OTHER_ALGORITHMIC_PROBLEM;deleted", taskProperties.getProperty("remark.1.example"));
    }

    @Test
    public void testMultipleDeletions1() {
    	final Properties taskProperties = new Properties();
    	final String result = MutationGenerator.applyMutations(
    			taskProperties,
    			new Random(123),
    			3,
    			TESTCODE,
    			delete("test1", "test2", "test3"),
    			CompositeOptimization.empty());
    	assertEquals(
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        String a2 = \"test2\";\n"
                + "        return 42;\n"
                + "    }\n"
                + "}\n",
    			result);
    	assertEquals("4;OTHER_ALGORITHMIC_PROBLEM;deleted",
    			taskProperties.getProperty("remark.1.example"));
    	assertEquals("5;OTHER_ALGORITHMIC_PROBLEM;deleted",
    			taskProperties.getProperty("remark.2.example"));
    }

    @Test
    public void testMultipleDeletions2() {
    	final Properties taskProperties = new Properties();
    	final String result = MutationGenerator.applyMutations(
    			taskProperties,
    			new Random(-1),
    			3,
    			TESTCODE,
    			delete("test1", "test2", "test3"),
    			CompositeOptimization.empty());
    	assertEquals(
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        String a2 = \"test2\";\n"
                + "        return 42;\n"
                + "    }\n"
                + "}\n",
    			result);
    	assertEquals("4;OTHER_ALGORITHMIC_PROBLEM;deleted",
    			taskProperties.getProperty("remark.1.example"));
    	assertEquals("5;OTHER_ALGORITHMIC_PROBLEM;deleted",
    			taskProperties.getProperty("remark.2.example"));
    }

    @Test
    public void testDeleteOrChangeButNotBoth1() {
    	final Properties taskProperties = new Properties();
    	final String result = MutationGenerator.applyMutations(
    			taskProperties,
    			new Random(123),
    			6,
    			TESTCODE,
    			deleteOrChange("test1", "test2", "test3"),
    			CompositeOptimization.empty());
    	assertEquals(
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        String a1 = \"test1changed\";\n"
                + "        String a3 = \"test3\";\n"
                + "        return 42;\n"
                + "    }\n"
                + "}\n",
    			result);
    	assertEquals("4;OTHER_ALGORITHMIC_PROBLEM;changed", taskProperties.getProperty("remark.1.example"));
    	assertEquals("5;OTHER_ALGORITHMIC_PROBLEM;deleted", taskProperties.getProperty("remark.2.example"));
    }

    @Test
    public void testDeleteOrChangeButNotBoth2() {
    	final Properties taskProperties = new Properties();
    	final String result = MutationGenerator.applyMutations(
    			taskProperties,
    			new Random(1000),
    			6,
    			TESTCODE,
    			deleteOrChange("test1", "test2", "test3"),
    			CompositeOptimization.empty());
    	assertEquals(
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        String a2 = \"test2\";\n"
                + "        String a3 = \"test3changed\";\n"
                + "        return 42;\n"
                + "    }\n"
                + "}\n",
    			result);
    	assertEquals("4;OTHER_ALGORITHMIC_PROBLEM;deleted", taskProperties.getProperty("remark.1.example"));
    	assertEquals("5;OTHER_ALGORITHMIC_PROBLEM;changed", taskProperties.getProperty("remark.2.example"));
    }

    @Test
    public void testLineNumbersAreStillCorrectIfOptimizationDeletesLines() {
    	final Properties taskProperties = new Properties();
    	final String result = MutationGenerator.applyMutations(
    			taskProperties,
    			new Random(123),
    			1,
    			TESTCODE,
    			change("test3"),
    			new DeleteOptimization("test2"));
    	assertEquals(
                "class A {\n"
                + "\n"
                + "    public int a() {\n"
                + "        String a1 = \"test1\";\n"
                + "        String a3 = \"test3changed\";\n"
                + "        return 42;\n"
                + "    }\n"
                + "}\n",
    			result);
    	assertEquals("5;OTHER_ALGORITHMIC_PROBLEM;changed", taskProperties.getProperty("remark.1.example"));
    }

}
