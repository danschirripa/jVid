package com.javashell.video;

public interface ControlInterface {

	public boolean addSubscriber(ControlInterface cf);

	public void processControl(Object obj);

	public void removeControlEgressor(ControlInterface cf);

}
