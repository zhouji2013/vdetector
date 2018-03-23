/**
 * 
 */
package net.zhouji.vdetector.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import net.zhouji.vdetector.apps.DetectionJob;

/**
 * A Swing application to visualize the V-detection batch job's results; it also
 * assists user in case the job described by the properties file has issues.
 * 
 * @author Zhou
 * 
 */
public class ResultVisualizationPanel  extends JComponent implements ActionListener {
	private static final long serialVersionUID = -2194455055971733159L;
	
	// until there is real need to be more flexible, keep these thing final
	final static private int leftPadding = 50;
	final static private int rightPadding = 50;
	final static private int bottomPadding = 50;
	final static private int topPadding = 20;

	protected DetectionJob job = null;
	
	ResultVisualizationPanel (DetectionJob job) {
		this.job = job;
	}

	private int fontHeight = 0;
	private Graphics2D g2 = null;
	
	protected int width = 0;
	protected int height = 0;
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		g2 = (Graphics2D) g;
		g2.setBackground(Color.white);

		width = getWidth() - (leftPadding + rightPadding);
		height = getHeight()-(topPadding + bottomPadding);

		fontHeight = g2.getFontMetrics().getHeight();
	}
	
	protected void drawBox() {
		// draw the box/axes
		g2.translate(leftPadding, topPadding);
		g2.clearRect(0, 0, width, height);
		g2.setColor(Color.black);
		g2.drawRect(0, 0, width, height);
	}
	
	private String formatNumber(double d) {
		return String.format("%7.4f", d);
	}
	
	protected void setXTitle(String title) {
		g2.drawString(title, width/2, height+topPadding+fontHeight);
	}

	protected void setYTitle(String title) {
		g2.rotate(Math.PI*0.5);
		g2.drawString(title, height/2, fontHeight-leftPadding);
		g2.rotate(-Math.PI*0.5);
	}

	protected void setY2Title(String title) {
		g2.rotate(Math.PI*0.5);
		g2.drawString(title, height/2, -(width+leftPadding+5) );
		g2.rotate(-Math.PI*0.5);
	}

	protected void setXRange(double x1, double x2) {
		g2.drawString(formatNumber(x1), leftPadding, topPadding+height+fontHeight);
		g2.drawString(formatNumber(x2), leftPadding+width-rightPadding, topPadding+height+fontHeight);
	}

	protected void setYRange(double y1, double y2) {
		g2.drawString(formatNumber(y1), 5, height+topPadding);
		g2.drawString(formatNumber(y2), 5, topPadding+fontHeight);
	}

	protected void setYRange2(double y1, double y2) {
		g2.drawString(formatNumber(y1), width+rightPadding+5, height+topPadding);
		g2.drawString(formatNumber(y2), width+rightPadding+5, topPadding+fontHeight);
	}

	protected int selectedIndex=0;
	public void actionPerformed(ActionEvent arg0) {
		Object source = arg0.getSource();
		if(source instanceof JComboBox) {
			JComboBox combo = (JComboBox)source;
			selectedIndex = combo.getSelectedIndex();
			repaint();
		}
	}
	
	protected static Color detectionRateStrokeColor = Color.blue;
	protected static Color detectionRateFillColor = new Color(200, 200, 255);
	protected static Color falseAlarmStrokeColor = Color.red;
	protected static Color falseAlarmFillColor = new Color(255, 200, 200);
}
