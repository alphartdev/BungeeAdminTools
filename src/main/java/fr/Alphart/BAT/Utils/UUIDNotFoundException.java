package fr.Alphart.BAT.Utils;


public class UUIDNotFoundException extends RuntimeException{
	private static final long serialVersionUID = 1L;
	private String player;
	
	public UUIDNotFoundException(String player){
		this.player = player;
	}
	
	public String getInvolvedPlayer(){
		return player;
	}
}
