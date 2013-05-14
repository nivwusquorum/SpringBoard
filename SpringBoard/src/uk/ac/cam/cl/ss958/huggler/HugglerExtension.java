package uk.ac.cam.cl.ss958.huggler;

public abstract class HugglerExtension {
	protected HugglerProtocol protocol = null;
	
	public void setProtocol(HugglerProtocol hp) {
		protocol = hp;
	}
	
	public HugglerProtocol getProtocol() {
		return protocol;
	}
	
	public abstract void create() ;
	public abstract void start();
	public abstract void stop();
	public abstract void destroy();

	public abstract void askNow();
}
