package org.openjump.core.rasterimage.sextante.rasterWrappers;

public class GridCell implements Comparable{

	private int m_iX, m_iY;
	private double m_dValue;
	
	public GridCell(int iX, int iY, double dValue){
		
		m_iX = iX;
		m_iY = iY;
		m_dValue = dValue;
		
	}
	
	public double getValue() {
		
		return m_dValue;
		
	}
	public void setValue(double dValue) {
		
		m_dValue = dValue;
		
	}
	
	public int getX() {
		
		return m_iX;
		
	}
	
	public void setX(int iX) {
		
		m_iX = iX;
		
	}
	
	public int getY() {
		
		return m_iY;
		
	}
	
	public void setY(int iY) {
		
		m_iY = iY;
		
	}
	
	 public int compareTo(Object cell) throws ClassCastException {
		 
		 if (!(cell instanceof GridCell)){
			 throw new ClassCastException();
		 }
		 
		 double dValue = ((GridCell) cell).getValue();
		 double dDif = this.m_dValue - dValue;   
		 
		 if (dDif > 0.0){
			 return 1;
		 }
		 else if (dDif < 0.0){
			 return -1;
		 }
		 else{
			 return 0;
		 }
	 
	 }
	 	 
	 public boolean equals(Object obj){
		 
		 if (obj instanceof GridCell){
			 GridCell cell = (GridCell) obj; 
			 return (m_iX == cell.getX() &&
					 m_iY == cell.getY() &&
					 m_dValue == cell.getValue()); 
		 }
		 else{
			 return false;
		 }
		 
	 }
	

}
