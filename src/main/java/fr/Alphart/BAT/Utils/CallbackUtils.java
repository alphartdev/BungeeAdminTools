package fr.Alphart.BAT.Utils;


public class CallbackUtils {
	
	public static interface Callback<T>{
		public void done(final T result);
	}
	
	public static class VoidCallback implements Callback<Object>{

		@Override
		public final void done(final Object nullable) {
			
		}
		
	}
}
