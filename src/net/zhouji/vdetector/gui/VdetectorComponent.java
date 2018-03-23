/**
 * 
 */
package net.zhouji.vdetector.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import net.zhouji.vdetector.Detector;
import net.zhouji.vdetector.RealVectorPoint;
/**
 * @author zji
 *
 */
public class VdetectorComponent extends JComponent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 715436358162104577L;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				try {
					createGui();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}
	
	static private void createGui() throws IOException {
		JFrame mainFrame = new JFrame("V-detector Demo");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.add(new VdetectorComponent("Pentagram-mid_train.txt", "detectorSet.ser"));
		mainFrame.setPreferredSize(new Dimension(600, 600));
		mainFrame.pack();
		mainFrame.setLocationRelativeTo(null);
		mainFrame.setVisible(true);
	}

	@Override
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		
		g2.setBackground(Color.white);
		g2.clearRect(0, 0, getWidth(), getHeight());
		
		double scale = Math.min(getWidth(), getHeight())/1.4;
		g2.scale(scale, scale);
		double translateX = 0.2; 
		double translateY = 0.2;
		double ar = ((double)getWidth())/getHeight(); 
		if(ar>1.)
			translateX += (getWidth()-getHeight())*0.5/scale;
		else
			translateY += (getHeight()-getWidth())*0.5/scale;
		g2.translate(translateX, translateY);

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	            RenderingHints.VALUE_ANTIALIAS_ON);
		
		g2.setStroke(new BasicStroke((float)(1./scale)));
		g.setColor(Color.red);
		double r = 2./scale;
		for(Point2D.Double p: trainingData) {
			Ellipse2D.Double point = new Ellipse2D.Double(p.x-r, p.y-r, 2.*r, 2.*r);
			g2.draw(point);
		}

		Composite c = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
			       .4f);
		g2.setComposite(c);

		for(Detector<Double> detector: detectorSet) {
			RealVectorPoint center = (RealVectorPoint)detector.getCenter();
			double radius = Math.sqrt( detector.getRadius() );
			double[] data = center.getData();
			Ellipse2D.Double d = new Ellipse2D.Double(data[0]-radius, data[1]-radius, 2.*radius, 2.*radius);
			g2.setColor(Color.yellow);
			g2.fill(d);
			g2.setColor(Color.blue);
			g2.draw(d);
		}

		g2.setColor(Color.blue);
		Rectangle2D.Double sq = new Rectangle2D.Double();
		sq.setFrame(-1, -1, 3, 3);
		Area st1 = new Area(sq);
		sq.setFrame(0, 0, 1, 1);
		Area st2 = new Area(sq);
		st1.subtract(st2);
		g2.fill(st1);
	}
	
	private List<Point2D.Double> importTrainingData(String filename) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(filename));

		String line = br.readLine();
		if(line==null) {
			System.out.println("It failed to read the header line of the training data file: ");
			throw new IOException("failed to read the header line of the training data file");
		}
		String[] token = line.split("\\s");
		int dim = Integer.parseInt(token[0]);
		int setSize = Integer.parseInt(token[1]);
		
		double[] rawData = new double[dim];
		
		List<Point2D.Double> training = new ArrayList<Point2D.Double>();
		for(int i=0; i<setSize; i++) {
			line = br.readLine();
			token = line.split("\\s");
		
			for(int j=0; j<dim; j++) {
				rawData[j] = Double.parseDouble(token[j]);
			}
			training.add( new Point2D.Double(rawData[0], rawData[1]));
		}
		return training;
	}
	
	private List<Detector<Double>> readSerializedDetectorSet(String filename) {
		List<Detector<Double>> detectors = null;
		FileInputStream fis = null;
		ObjectInputStream in = null;
		try {
			fis = new FileInputStream(filename);
			in = new ObjectInputStream(fis);
			detectors = (List<Detector<Double>>) in.readObject();
			in.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		return detectors;
	}
	
	private List<Point2D.Double> trainingData = null;
	private List<Detector<Double>> detectorSet = null;
	
	VdetectorComponent(String trainingDataFile, String detectorSetFile) {
		try {
			trainingData = importTrainingData(trainingDataFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		detectorSet = readSerializedDetectorSet(detectorSetFile);
	}
	
	VdetectorComponent(List<Point2D.Double> trainingData, List<Detector<Double>> detectorSet) {
		this.trainingData = trainingData;
		this.detectorSet = detectorSet;
	}

	public void setDetectorSet(List<Detector<Double>> detectorSet) {
		this.detectorSet = detectorSet;
	}
}
