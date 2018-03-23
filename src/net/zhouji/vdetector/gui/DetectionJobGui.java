/**
 * 
 */
package net.zhouji.vdetector.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;

import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import net.zhouji.vdetector.apps.DetectionJob;
import net.zhouji.vdetector.apps.DetectionJob.EvaluationResult;
import net.zhouji.vdetector.apps.DetectionJob.OneEvaluationResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;

/**
 * A Swing application to visualize the V-detection batch job's results; it also
 * assists user in case the job described by the properties file has issues.
 * 
 * @author Zhou
 * 
 */
public class DetectionJobGui {
	final static String PRODUCT_FULL_NAME = "V-detection Batch Job";
	static Log log = LogFactory.getLog(DetectionJobGui.class);

	static DetectionJob job = null;
	static String errorMessage = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			job = new DetectionJob(new File("parameters.properties"));
		} catch (Exception e) {
			// not correct properties file, we need (1) give the default
			// parameters and (2) prompt for files
			e.printStackTrace();
			int n = JOptionPane
					.showConfirmDialog(
							null,
							"There is program opening job descition from the propeerties file. Do you want to try to open another properties file?",
							"Open job properties", JOptionPane.YES_NO_OPTION);
			if (n == JOptionPane.YES_OPTION) {
				JFileChooser fc = new JFileChooser();
				int returnVal = fc.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					try {
						job = new DetectionJob(file);
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} finally {
						if (job == null) {
							log
									.error("Failure one the second try of opeing job description properties file.");
						}
						System.exit(0);
					}
				} else {
					System.exit(0);
				}
			} else {
				System.exit(0);
			}
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});

		// if algorithm is invoked from GUI event instead automatically like
		// here, it needs to start from a back-end thread and leave main thread
		// finish by itself quickly
		// TODO the command line option e should be replace by some GUI option
		// and the default should be evaluation instead of detection
		if (args.length ==0 || args[0].equalsIgnoreCase("e")) {
			errorMessage = null;
			try {
				job.evaluate();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				errorMessage = e.getMessage();
			}
			// update GUI
			// no need to block main thread - back-end thread, Swing does what it
			// needs to do
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					mainFrame.updateConsoleMessage(job.htmlEvaluationResults());
					JPanel chartPanel = new JPanel();
					chartPanel.setLayout(new BorderLayout());
					JComboBox combo = new JComboBox();
					for (EvaluationResult resultsForOneDataset : job
							.getAllEvaluationResults()) {
						combo.addItem(resultsForOneDataset
								.getTrainingDataFile()
								+ " | "
								+ resultsForOneDataset.getTestDataFile());
					}
					chartPanel.add(combo, BorderLayout.NORTH);
					VdJobChartPanel chartPanelPlot = new VdJobChartPanel(job);
					combo.addActionListener(chartPanelPlot);
					chartPanel.add(chartPanelPlot, BorderLayout.CENTER);
					mainFrame.tabbedPane
							.addTab("Performance Chart", chartPanel);

					JPanel mapPanel = new JPanel();
					mapPanel.setLayout(new BorderLayout());
					JComboBox combo2 = new JComboBox();
					for (EvaluationResult resultsForOneDataset : job
							.getAllEvaluationResults()) {
						combo2.addItem(resultsForOneDataset
								.getTrainingDataFile()
								+ " | "
								+ resultsForOneDataset.getTestDataFile());
					}
					mapPanel.add(combo2, BorderLayout.NORTH);
					VdJobMapPanel mapPanelPlot = new VdJobMapPanel(job);
					combo2.addActionListener(mapPanelPlot);
					mapPanel.add(mapPanelPlot, BorderLayout.CENTER);
					mainFrame.tabbedPane.addTab("Performance Map",
							mapPanel);
				}
			});
			try {
				job.saveEvaluationResults();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				errorMessage = e.getMessage();
			}
			// something went wrong. quit
			if(errorMessage!=null) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						mainFrame
								.updateConsoleMessage(errorMessage);
					}
				});
				return;
			}
		} else { // detection case
			errorMessage = null;
			try {
				job.detect();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				errorMessage = e.getMessage();
			}
			// update GUI
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					mainFrame
							.updateConsoleMessage(job.formatDetectionResults());
					// only the first tab (console) should be left
					mainFrame.tabbedPane.removeTabAt(1);
					mainFrame.tabbedPane.removeTabAt(2);
				}
			});
			try {
				job.saveDetectionResults();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				errorMessage = e.getMessage();
			}
			// something went wrong. quit
			if(errorMessage!=null) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						mainFrame
								.updateConsoleMessage(errorMessage);
					}
				});
				return;
			}
		}
	}

	static DetectionJobFrame mainFrame = null;

	private static void createAndShowGUI() {
		mainFrame = new DetectionJobFrame(PRODUCT_FULL_NAME);
	}
}

// TODO to refactor later. for the initial prototype, start in the same file
class DetectionJobFrame extends JFrame {
	private static final long serialVersionUID = 3721319011290977336L;

	void updateConsoleMessage(String m) {
		consolePane.setText(m);
	}

	void appendConsoleMessage(String m) {
		consolePane.setText(consolePane.getText() + m);
	}

	JTabbedPane tabbedPane = null;
	private JEditorPane consolePane = null;

	DetectionJobFrame(String title) {
		super(title);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		tabbedPane = new JTabbedPane();
		tabbedPane.setPreferredSize(new Dimension(600, 600));

		consolePane = new JEditorPane(
				"text/html",
				"Welcome to V-detector, a Negative Selection Algorithm for one-class classification.<p>"
						+ "This is the batch job GUI. If you have any questions on this program, please contact the author Zhou Ji at "
						+ "<a href=mailto:zhou.ji@yahoo.com>zhou.ji@yahoo.com</a>.");
		tabbedPane.addTab("Console", new JScrollPane(consolePane));

		add(tabbedPane, BorderLayout.CENTER);

		//
		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic('H');
		JMenuItem aboutItem = new JMenuItem("About", KeyEvent.VK_A);
		aboutItem.addActionListener(new ActionListener() {
			// @Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(DetectionJobFrame.this,
						DetectionJobGui.PRODUCT_FULL_NAME
								+ "\n(c) Zhou Ji 2009");

			}
		});
		helpMenu.add(aboutItem);

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(helpMenu);
		setJMenuBar(menuBar);

		pack();
		setLocationRelativeTo(null); // this is cool - default to the middle
										// of the screen
		setVisible(true);

	}
}

/**
 * Chart to show detection rate and false alarm rate for one pair of training
 * data and test data.
 */
class VdJobChartPanel extends ResultVisualizationPanel {
	private static final long serialVersionUID = 8878491032859772969L;
	
	private double minDr;
	private double maxDr = 1.;
	private double minFa = 0, maxFa;
	
	VdJobChartPanel(DetectionJob job) {
		super(job);

		/* find the minimum y value needed on the plot. can we do this more efficiently? */
		minDr = 1;
		maxFa = 0;
		for (EvaluationResult r : job.getAllEvaluationResults()) {
			for (OneEvaluationResult result : r.getOneResult()) {
				SummaryStatistics s = result.getStatsDr();
				double drMin = s.getMin();
				if(drMin<minDr)minDr = drMin;
				if(result.getStatsFa().getMax()>maxFa)maxFa = result.getStatsFa().getMax();
			}
		}
		minDr -= 0.1;
		maxDr = 1.;
		
		maxFa += 0.1;
		minFa = 0;
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D) g;
		g2.setBackground(Color.white);

		int numParam = job.getNumberParameterSettings(); 
		int deltaX = width / numParam;

		g2.setColor(Color.blue);
		setYRange(minDr, maxDr);
		g2.setColor(Color.red);
		setYRange2(minFa, maxFa);
		g2.setColor(Color.black);
		setXTitle("Parameter Settings");
		g2.setColor(Color.blue);
		setYTitle("Detection Rate");
		g2.setColor(Color.red);
		setY2Title("False Alarm Rate");

		drawBox();
		
		// do detection rate
		for (int i = 0; i < numParam; i++) {
			int x = i*deltaX;
			g2.drawLine(x, 0, x, height);
		}
		
		g2.translate(0., height);
		g2.scale(1., height/(minDr-maxDr));
		g2.translate(0., -minDr);
		
		EvaluationResult resultsForOneDataset =  job.getAllEvaluationResults().get(selectedIndex);
			int i=0;
			for(OneEvaluationResult result: resultsForOneDataset.getOneResult() ) {
			double x = i*deltaX;
			double x1 = (i+1)*deltaX;
			
			SummaryStatistics s = result.getStatsDr();
			double drMin = s.getMin();
			double drMax = s.getMax();
			double drMean = s.getMean();
			double drSd = s.getStandardDeviation();

			g2.setColor(new Color(175, 175, 200));
			Rectangle2D r2 = new Rectangle2D.Double(x, drMean-drSd, deltaX, drSd*2); 
			g2.fill(r2); 
			
			g2.setColor(Color.blue);
			float yScale = (float)(height/(maxDr-minDr));
			g2.setStroke(new BasicStroke(1/yScale)); 
			g2.draw(new Line2D.Double(x, drMean, x1, drMean));
			Rectangle2D r3 = new Rectangle2D.Double(x, drMin, deltaX, drMax-drMin);
			g2.draw(r3);
			i++;
		} 

		// do false alarm rate
		g2.translate(0., minDr);
		g2.scale(1., (minDr - maxDr) / (minFa - maxFa));
		g2.translate(0., minFa);

		EvaluationResult r = job.getAllEvaluationResults().get(selectedIndex);
		i = 0;
		for (OneEvaluationResult result : r.getOneResult()) {
			double x = i * deltaX;
			double x1 = (i + 1) * deltaX;

			SummaryStatistics s = result.getStatsFa();
			double faMin = s.getMin();
			double faMax = s.getMax();
			double faMean = s.getMean();
			double faSd = s.getStandardDeviation();

			g2.setColor(new Color(200, 150, 150));
			Rectangle2D r2 = new Rectangle2D.Double(x, faMean - faSd, deltaX,
					faSd * 2);
			g2.fill(r2);

			g2.setColor(Color.pink);
			float yScale = (float) (height / (maxFa - minFa));
			g2.setStroke(new BasicStroke(1 / yScale));
			g2.draw(new Line2D.Double(x, faMean, x1, faMean));
			Rectangle2D r3 = new Rectangle2D.Double(x, faMin, deltaX, faMax
					- faMin);
			g2.draw(r3);
			i++;
		}

	}
}

class VdJobMapPanel extends ResultVisualizationPanel {
	private static final long serialVersionUID = -4147509193150913375L;
	
	// to make it simple, use constant scale for the size of the circle, both
	// for detection rate and false alarm rate
	final static private double dotScale = 0.1;

	private double lowerCoverage;
	private double upperCoverage = 1.;
	private double lowerThreshold = 0.;
	private double upperThreshold;
	
	VdJobMapPanel(DetectionJob job) {
		super(job);

		/* find the minimum y value needed on the plot. can we do this more efficiently? */
		lowerCoverage = 1.;
		upperThreshold = 0.;
		for (EvaluationResult r : job.getAllEvaluationResults()) {
			for (OneEvaluationResult result : r.getOneResult()) {
				double coverage = result.getCoverage();
				if(coverage<lowerCoverage)lowerCoverage = coverage;
				double threshold = result.getThreshold();
				if(threshold>upperThreshold)upperThreshold = threshold;
			}
		}
		// leave some extra space
		lowerCoverage -= 0.02;
		upperThreshold += 0.1;
		
		// leave the following two as constants for now
		lowerThreshold = 0.;
		upperCoverage = 1.;
	}

	private int panelScale = 0;
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D) g;
		g2.setBackground(Color.white);

		panelScale = Math.min(width, height);

		g2.setColor(Color.black);
		setXTitle("Threshold");
		setYTitle("Target Coverage");
		
		setXRange(lowerThreshold, upperThreshold);
		setYRange(lowerCoverage, upperCoverage);
		
		drawBox();
		
		EvaluationResult resultsForOneDataset =  job.getAllEvaluationResults().get(selectedIndex);
		for(OneEvaluationResult result: resultsForOneDataset.getOneResult() ) {
			double coverage = result.getCoverage();
			double threshold = squareRootThreshold( result.getThreshold() );

			// do manual transformation so the width-1 circle will be OK
			threshold = (threshold-lowerThreshold)*width/(upperThreshold-lowerThreshold);
			coverage = (upperCoverage-coverage)*height/(upperCoverage-lowerCoverage);
			
			double radius = 0;

			SummaryStatistics dr = result.getStatsDr();
			double drMean = dr.getMean();
			double drMin = dr.getMin();
			double drMax = dr.getMax();
			double drSd = dr.getStandardDeviation();
			
			g2.setColor(detectionRateFillColor);
			g2.setStroke(new BasicStroke((float)scaleForMapPanel(drSd+drSd)));
			radius = scaleForMapPanel(drMean);
			drawCircle(threshold, coverage, radius, g2);
			
			g2.setColor(detectionRateStrokeColor);
			g2.setStroke(new BasicStroke());
			drawCircle(threshold, coverage, radius, g2);
			drawCircle(threshold, coverage, scaleForMapPanel(drMin), g2);
			drawCircle(threshold, coverage, scaleForMapPanel(drMax), g2);
			
			// separate dr section and fa section so easier to see wrong variables
			SummaryStatistics fa = result.getStatsFa();
			double faMean = fa.getMean();
			double faMin = fa.getMin();
			double faMax = fa.getMax();
			double faSd = fa.getStandardDeviation();

			g2.setColor(falseAlarmFillColor);
			g2.setStroke(new BasicStroke((float)scaleForMapPanel(faSd+faSd)));
			radius = scaleForMapPanel(faMean);
			drawCircle(threshold, coverage, radius, g2);
			
			g2.setColor(falseAlarmStrokeColor);
			drawCircle(threshold, coverage, radius, g2);
			drawCircle(threshold, coverage, scaleForMapPanel(faMin), g2);
			drawCircle(threshold, coverage, scaleForMapPanel(faMax), g2);
		}
	}
	
	private double scaleForMapPanel(double x) {
		return x * dotScale * panelScale;
	}
	
	private void drawCircle(double x, double y, double radius, Graphics2D g2) {
		double d = radius+radius;
		g2.draw(new Ellipse2D.Double(x-radius, y-radius, d, d));
	}

	private double squareRootThreshold(double threshold2) {
		return Math.sqrt(threshold2);
	}
}
