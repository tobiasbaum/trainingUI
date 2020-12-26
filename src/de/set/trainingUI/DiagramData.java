package de.set.trainingUI;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

public class DiagramData {

	public static CategoryDataset getTasksPerWeek(Trainee t) {
		final Map<String, List<Trial>> trialsPerWeek = getTrialsPerWeek(t);
    	final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    	for (final Entry<String, List<Trial>> e : trialsPerWeek.entrySet()) {
    		dataset.addValue(e.getValue().size(), "", e.getKey());
    	}
		return dataset;
	}

	public static CategoryDataset getCorrectnessPerWeek(Trainee t) {
		final Map<String, List<Trial>> trialsPerWeek = getTrialsPerWeek(t);
    	final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    	for (final Entry<String, List<Trial>> e : trialsPerWeek.entrySet()) {
    		dataset.addValue(determineRelativeCorrectness(e.getValue()), "", e.getKey());
    	}
		return dataset;
	}

	public static CategoryDataset getDurationPerWeek(Trainee t) {
		final Map<String, List<Trial>> trialsPerWeek = getTrialsPerWeek(t);
    	final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    	for (final Entry<String, List<Trial>> e : trialsPerWeek.entrySet()) {
    		dataset.addValue(determineTrimmedMeanCorrectDuration(e.getValue()), "", e.getKey());
    	}
		return dataset;
	}

	public static CategoryDataset getTrainingDurationPerWeek(Trainee t) {
		final Map<String, List<Trial>> trialsPerWeek = getTrialsPerWeek(t);
    	final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    	for (final Entry<String, List<Trial>> e : trialsPerWeek.entrySet()) {
    		dataset.addValue(determineDurationSum(e.getValue()), "", e.getKey());
    	}
		return dataset;
	}

	private static double determineRelativeCorrectness(List<Trial> trials) {
		int correct = 0;
		int incorrect = 0;
		for (final Trial t : trials) {
			if (t.isCorrect()) {
				correct++;
			} else if (t.isIncorrect()) {
				incorrect++;
			}
		}
		if (correct == 0) {
			return 0.0;
		} else {
			return 100.0 * correct / (correct + incorrect);
		}
	}

	private static double determineTrimmedMeanCorrectDuration(List<Trial> trials) {
		final List<Long> durations = new ArrayList<>();
		for (final Trial t : trials) {
			if (t.isCorrect()) {
				durations.add(t.getNeededTime());
			}
		}
		return trimmedMean(durations);
	}

	private static double trimmedMean(List<Long> durations) {
		if (durations.isEmpty()) {
			return 0.0;
		}
		Collections.sort(durations);
		final int trim = durations.size() / 10;
		double sum = 0.0;
		for (int i = trim; i < durations.size() - trim; i++) {
			sum += durations.get(i).doubleValue();
		}
		return sum / (durations.size() - 2 * trim);
	}

	private static double determineDurationSum(List<Trial> trials) {
		long sum = 0;
		for (final Trial t : trials) {
			if (t.isFinished()) {
				sum += t.getNeededTime();
			}
		}
		return sum / 60.0;
	}

	private static Map<String, List<Trial>> getTrialsPerWeek(Trainee trainee) {
		final Map<String, List<Trial>> ret = new TreeMap<>();

		// Trials den Wochen zuordnen
		Instant minDate = null;
		for (final Trial trial : trainee.getTrials()) {
			final String week = getWeek(trial.getStartTime());
			List<Trial> list = ret.get(week);
			if (list == null) {
				list = new ArrayList<>();
				ret.put(week, list);
			}
			list.add(trial);
			if (minDate == null || minDate.isAfter(trial.getStartTime())) {
				minDate = trial.getStartTime();
			}
		}

		// fehlende Wochen ergänzen
		if (minDate != null) {
			final Instant now = Instant.now();
			while (minDate.isBefore(now)) {
				final String week = getWeek(minDate);
				if (!ret.containsKey(week)) {
					ret.put(week, Collections.emptyList());
				}
				minDate = minDate.plus(7, ChronoUnit.DAYS);
			}
		}

		return ret;
	}

	private static String getWeek(Instant instant) {
		final ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
		return String.format("%2d/%d", zdt.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), zdt.getYear());
	}

}