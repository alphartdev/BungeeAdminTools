package fr.Alphart.BAT.Modules;

public class InvalidModuleException extends Exception {
	private static final long serialVersionUID = 1L;

	private final String message;
		public String  getMessage() { return message; }
	
	public InvalidModuleException(final String message){
		this.message = message;
	}
}
