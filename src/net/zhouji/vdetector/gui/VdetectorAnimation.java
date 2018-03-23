/**
 * 
 */
package net.zhouji.vdetector.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import net.zhouji.vdetector.Detector;
import net.zhouji.vdetector.DetectorGenerator;
import net.zhouji.vdetector.HypothesisSetting;
import net.zhouji.vdetector.NaiveSetting;
import net.zhouji.vdetector.RealVectorDetectorGenerator;
import net.zhouji.vdetector.RealVectorPoint;
import net.zhouji.vdetector.Setting;
import net.zhouji.vdetector.VdPoint;
import net.zhouji.vdetector.DetectorGenerator.VDetectorParameterException;
import net.zhouji.vdetector.apps.DetectionJob;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A Swing application to visualize the V-detection batch job's results; it also
 * assists user in case the job described by the properties file has issues.
 * 
 * @author Zhou
 * 
 */
public class VdetectorAnimation {
	final static String PRODUCT_FULL_NAME = "V-detection Animation";
	static Log log = LogFactory.getLog(VdetectorAnimation.class);

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
				try {
					createAndShowGUI();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		errorMessage = null;

	}

	static AnimationFrame mainFrame = null;

	static VdetectorComponent vdetectorComponent = null;
	private static void createAndShowGUI() throws Exception {
		mainFrame = new AnimationFrame(PRODUCT_FULL_NAME);

		Setting<Double> setting = null;
		DetectorGenerator<Double> generator = null;
		if (job.isHypothesisTesting()) {
			setting = new HypothesisSetting<Double>(job.getCoverage()[0], // coverage
					new Double(job.getThresholds()[0]), // threshold
					job.getTrainingFileNames()[0], // filename
					job.getLimit()[0], // numberLimit
					0.9); // alpha
			generator = new RealVectorDetectorGenerator(
					(HypothesisSetting<Double>) setting);
		} else {
			setting = new NaiveSetting<Double>(job.getCoverage()[0], // coverage
					new Double(job.getThresholds()[0]), // threshold
					job.getTrainingFileNames()[0], // filename
					job.getLimit()[0] // number of detectors
			);
			generator = new RealVectorDetectorGenerator(
					(NaiveSetting<Double>) setting);
		}
		List<Point2D.Double> trainingData = new ArrayList<Point2D.Double>();
		for(VdPoint<Double> p: generator.getTrainingData()) {
			RealVectorPoint rp = (RealVectorPoint)p;
			double[] x = rp.getData();
			trainingData.add(new Point2D.Double(x[0], x[1]));
		}

		// add an empty one first
		vdetectorComponent = new VdetectorComponent(
				trainingData,
				new ArrayList<Detector<Double>>()) ; 
		vdetectorComponent.setPreferredSize(new Dimension(600, 600));
		
		mainFrame.add(vdetectorComponent);
		mainFrame.pack();
		mainFrame.setLocationRelativeTo(null); // this is cool - default to the middle
		// of the screen
		mainFrame.setVisible(true);
		

		/*
		 * evaluate is too much for here - but try to reuse code from 'job' as
		 * much as I can here
		 */
		// make this part a swing worker
		// end of swingWorker to be
		VdetectotWorker task = new VdetectotWorker(job, mainFrame, vdetectorComponent, generator);
		task.execute();
	}
}

class VdetectotWorker extends SwingWorker<Void, List<Detector<Double>>> {

	VdetectorComponent vdetectorComponent = null;
	AnimationFrame mainFrame = null;
	DetectionJob job;
	Setting<Double> setting = null;
	DetectorGenerator<Double> generator = null;
	List<Detector<Double>> detectorSet = null;

	VdetectotWorker(DetectionJob job, AnimationFrame mainFrame, VdetectorComponent vdetectorComponent, DetectorGenerator<Double> generator) {
		this.job = job;
		this.mainFrame = mainFrame;
		this.vdetectorComponent = vdetectorComponent;
		this.generator = generator;
	}

	@SuppressWarnings("unchecked") // for publish(detectors);
	@Override
	protected Void doInBackground() throws Exception {
		final int MAX_SAMPLING = 1000000;
		generator.resetDemo();
		try {
			List<Detector<Double>> detectors = new ArrayList<Detector<Double>>();
			int totalSampling = 0;
			boolean enough = true;
			int s = detectors.size();
			do {
				// 1: generate a detector
				enough = generator.tryToGenerateOneDetector(detectors);
				totalSampling++;

				if(detectors.size()>s) {
					// 2: update GUI
					publish(detectors);
					s = detectors.size();
					// 3: sleep 2 seconds
					Thread.sleep(2000);
				}
			} while (!enough && totalSampling<MAX_SAMPLING);
		
			if(totalSampling==MAX_SAMPLING)
				log.info("I'm tired of this. I sampled "+MAX_SAMPLING+" times!");
			
			log.info("Total number of detectors is "+detectors.size());
			

		} catch (VDetectorParameterException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// return nothing
		return null;
	}
	private Log log = LogFactory.getLog(VdetectorAnimation.class);


	@Override
	protected void done() {
		System.out.println("All done - animation finished.");
	}

	@Override
	protected void process(List<List<Detector<Double>>> chunks) {
		// only draw the last list
		List<Detector<Double>> listToDraw = chunks.get(chunks.size()-1);
		vdetectorComponent.setDetectorSet(listToDraw);
		mainFrame.repaint();
	}
}

class AnimationFrame extends JFrame {
	private static final long serialVersionUID = 3721319011290977336L;

	AnimationFrame(String title) {
		super(title);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		//
		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic('H');
		JMenuItem aboutItem = new JMenuItem("About", KeyEvent.VK_A);
		aboutItem.addActionListener(new ActionListener() {
			// @Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(AnimationFrame.this,
						VdetectorAnimation.PRODUCT_FULL_NAME
								+ "\n(c) Zhou Ji 2009");

			}
		});
		helpMenu.add(aboutItem);

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(helpMenu);
		setJMenuBar(menuBar);
	}
}
