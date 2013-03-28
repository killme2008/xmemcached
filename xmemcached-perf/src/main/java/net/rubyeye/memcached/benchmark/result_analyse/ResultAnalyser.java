package net.rubyeye.memcached.benchmark.result_analyse;

/**
 * Analyse result,and generate chart
 * @author dennis
 *
 */
import java.awt.Color;
import java.awt.Font;
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

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtilities;

/**
 * @author dennis
 */
public class ResultAnalyser {

	private static final int HEIGHT = 600;
	private static final String DEFAULT_RESULT_IMAGES_DIR = "result/images";
	private static final String DEFAULT_RESULT_DIR = "result";

	public static void main(String[] args) throws Exception {
		String resultDirName = DEFAULT_RESULT_DIR;
		String resultImagesDirName = DEFAULT_RESULT_IMAGES_DIR;
		if (args.length > 2) {
			resultDirName = args[0];
			resultImagesDirName = args[1];
		}

		File resultDir = new File(resultDirName);
		File imageDir = new File(resultImagesDirName);
		if (!resultDir.exists() || !resultDir.isDirectory()) {
			System.err.println("Benchmark result dir is not exist");
			System.err.println(1);
		}
		if (!imageDir.exists()) {
			imageDir.mkdir();
		}

		Map<String, Map<Integer, List<Double>>> result1 = parseResultForValueLength(resultDir);
		Map<String, Map<Integer, List<Double>>> result2 = parseResultForThreads(resultDir);

		drawChartByLength(imageDir, result1);
		drawChartByThreads(imageDir, result2);
		System.out.println("done");

	}

	private static void drawChartByLength(File imageDir,
			Map<String, Map<Integer, List<Double>>> result) {
		final String[] rowKeys = getRowKeys(result);
		StringBuilder sb = getCommonChartTitile(rowKeys);
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
						if (j >= list.size()) {
							data[i][j] = 0;
						} else {
							data[i][j] = list.get(j);
						}
					}

				}
			}
			CategoryDataset dataset = createDataset(rowKeys, colKeys, data);
			JFreeChart freeChart = createChart(dataset, "Threads", "TPS",
					"Size=" + valueLength + " Bytes");
			saveAsFile(freeChart, imageDir + "/bytes" + valueLength + ".jpg",
					(int) (HEIGHT * 1.6), HEIGHT);
		}
	}

	private static void drawChartByThreads(File imageDir,
			Map<String, Map<Integer, List<Double>>> result) {
		final String[] rowKeys = getRowKeys(result);
		StringBuilder sb = getCommonChartTitile(rowKeys);
		final String[] colKeys = new String[Constants.BYTES.length];
		for (int i = 0; i < colKeys.length; i++) {
			colKeys[i] = String.valueOf(Constants.BYTES[i]);
		}

		for (int threads : Constants.THREADS) {
			double[][] data = new double[rowKeys.length][colKeys.length];
			for (int i = 0; i < rowKeys.length; i++) {
				final List<Double> list = result.get(rowKeys[i]).get(threads);
				if (list != null) {
					for (int j = 0; j < data[i].length; j++) {
						if (j >= list.size()) {
							data[i][j] = 0;
						} else {
							data[i][j] = list.get(j);
						}
					}

				}
			}
			CategoryDataset dataset = createDataset(rowKeys, colKeys, data);
			JFreeChart freeChart = createChart(dataset, "Value Size(bytes)",
					"TPS", "Threads=" + threads);
			saveAsFile(freeChart, imageDir + "/threads" + threads + ".jpg",
					(int) (HEIGHT * 1.6), HEIGHT);
		}
	}

	private static StringBuilder getCommonChartTitile(final String[] rowKeys) {
		StringBuilder sb = new StringBuilder();
		boolean wasFirst = true;
		for (String rowKey : rowKeys) {
			if (wasFirst) {
				sb.append(rowKey);
				wasFirst = false;
			} else {
				sb.append(" VS. ").append(rowKey);
			}

		}
		return sb;
	}

	private static String[] getRowKeys(
			Map<String, Map<Integer, List<Double>>> result) {
		final String[] rowKeys = result.keySet().toArray(
				new String[result.keySet().size()]);
		return rowKeys;
	}

	private static Map<String, Map<Integer, List<Double>>> parseResultForValueLength(
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
							tps = new ArrayList<Double>(
									Constants.THREADS.length);
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

	private static Map<String, Map<Integer, List<Double>>> parseResultForThreads(
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

						Map<Integer, List<Double>> thrads2tps = result
								.get(memcachedClientName);
						int threads = extractVar("threads", line);
						List<Double> tps = thrads2tps.get(threads);
						if (tps == null) {
							tps = new ArrayList<Double>(Constants.BYTES.length);
							thrads2tps.put(threads, tps);
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

	public static JFreeChart createChart(CategoryDataset categoryDataset,
			String rowName, String colName, String chartTitle) {

		JFreeChart jfreechart = ChartFactory.createLineChart(chartTitle,
				rowName, colName, categoryDataset, PlotOrientation.VERTICAL,
				true, false, // tooltips
				false); // URLs

		LegendTitle legend = jfreechart.getLegend();

		legend.setItemFont(new Font("Dotum", Font.BOLD, 16));

		CategoryPlot plot = (CategoryPlot) jfreechart.getPlot();

		LineAndShapeRenderer render = (LineAndShapeRenderer) plot.getRenderer();
		render.setSeriesPaint(0, Color.RED);
		render.setSeriesPaint(1, Color.GREEN);
		render.setSeriesPaint(2, Color.YELLOW);
		render.setSeriesPaint(3, Color.BLUE);
		render.setSeriesPaint(4, Color.CYAN);
		render.setShapesFilled(Boolean.TRUE);// 在数据点显示实心的小图标
		render.setShapesVisible(true);// 设置显示小图标

		CategoryAxis cateaxis = plot.getDomainAxis();

		cateaxis.setLabelFont(new Font("Dotum", Font.BOLD, 16));

		cateaxis.setTickLabelFont(new Font("Dotum", Font.BOLD, 16));

		NumberAxis numaxis = (NumberAxis) plot.getRangeAxis();

		numaxis.setLabelFont(new Font("Dotum", Font.BOLD, 16));
		TextTitle title = new TextTitle(chartTitle);
		title.setFont(new Font("Dotum", Font.BOLD, 16));
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