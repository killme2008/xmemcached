package net.rubyeye.memcached.benchmark.result_analyse;

/**
 * Analyse result,and generate chart
 * @author dennis
 *
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.rubyeye.memcached.benchmark.Constants;

import org.jfree.chart.*;

import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.TextTitle;

import org.jfree.data.category.CategoryDataset;

import org.jfree.data.general.DatasetUtilities;

/**
 * @author dennis
 */
public class ResultAnalyser {

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.err
					.println("Useage:java ResultAnalyser [resultDir] [imageDir]");
			System.exit(1);
		}

		File resultDir = new File(args[0]);
		File imageDir = new File(args[1]);
		if (!resultDir.exists() || !resultDir.isDirectory()) {
			System.err.println("Benchmark result dir is not exist");
			System.err.println(1);
		}
		if (!imageDir.exists()) {
			imageDir.mkdir();
		}

		Map<String, Map<Integer, List<Double>>> result = parseResult(resultDir);

		drawChart(imageDir, result);

		System.out.println("done");

	}

	private static void drawChart(File imageDir,
			Map<String, Map<Integer, List<Double>>> result) {
		final String[] rowKeys = result.keySet().toArray(
				new String[result.keySet().size()]);
		final String[] colKeys = new String[Constants.THREADS.length];
		for (int i = 0; i < colKeys.length; i++) {
			colKeys[i] = String.valueOf(Constants.THREADS[i]);
		}

		for (int valueLength : Constants.BYTES) {
			double[][] data = new double[rowKeys.length][colKeys.length];
			for (int i = 0; i < rowKeys.length; i++) {
				final List<Double> list = result.get(rowKeys[i]).get(
						valueLength);
				if (list != null) {
					for (int j = 0; j < data[i].length; j++) {
						if (j >= list.size())
							data[i][j] = 0;
						else
							data[i][j] = list.get(j);
					}

				}
			}
			CategoryDataset dataset = createDataset(rowKeys, colKeys, data);
			JFreeChart freeChart = createChart(dataset);
			saveAsFile(freeChart, imageDir + "/value" + valueLength + ".jpg",
					651, 400);
		}
	}

	private static Map<String, Map<Integer, List<Double>>> parseResult(
			File resultDir) throws FileNotFoundException, IOException {
		Map<String, Map<Integer, List<Double>>> result = new HashMap<String, Map<Integer, List<Double>>>();
		String memcachedClientName = null;
		for (File file : resultDir.listFiles()) {
			if (file.isFile()) {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line = null;
				while ((line = reader.readLine()) != null) {
					if (line.indexOf("startup") > 0) {
						memcachedClientName = line.split(" ")[0].trim()
								.toLowerCase();
						if (result.get(memcachedClientName) == null) {
							result.put(memcachedClientName,
									new HashMap<Integer, List<Double>>());
						}

					} else if (line.startsWith("threads")) {
						Map<Integer, List<Double>> length2Tps = result
								.get(memcachedClientName);
						int valueLength = extractVar("valueLength", line);
						List<Double> tps = length2Tps.get(valueLength);
						if (tps == null) {
							tps = new ArrayList<Double>(6);
							length2Tps.put(valueLength, tps);
						}
						tps.add((double) extractVar("tps", line));
					}
				}
				reader.close();
			}
		}
		return result;
	}

	public static final int extractVar(final String varName, final String line) {
		Pattern pattern = Pattern.compile(".*" + varName + "=(\\d+).*");
		Matcher m = pattern.matcher(line);
		if (m.matches()) {
			return Integer.parseInt(m.group(1));
		}
		return -1;
	}

	public static void saveAsFile(JFreeChart chart, String outputPath,
			int weight, int height) {
		FileOutputStream out = null;
		try {
			java.io.File outFile = new java.io.File(outputPath);
			if (!outFile.getParentFile().exists()) {
				outFile.getParentFile().mkdirs();
			}
			out = new FileOutputStream(outputPath);
			ChartUtilities.writeChartAsJPEG(out, chart, weight, height);
			out.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}
	}

	public static JFreeChart createChart(CategoryDataset categoryDataset) {

		JFreeChart jfreechart = ChartFactory.createLineChart(
				"Xmemcached VS. Spymemcached", "threads", "TPS",
				categoryDataset, PlotOrientation.VERTICAL, true, false, // tooltips
				false); // URLs
		TextTitle title = new TextTitle("Xmemcached VS. Spymemcached");
		jfreechart.setTitle(title);

		return jfreechart;
	}

	/**
	 * 创建CategoryDataset对象
	 * 
	 */
	public static CategoryDataset createDataset(String[] rowKeys,
			String[] colKeys, double[][] data) {

		return DatasetUtilities.createCategoryDataset(rowKeys, colKeys, data);
	}
}