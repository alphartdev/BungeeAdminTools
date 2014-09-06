package fr.Alphart.BAT.Utils;


public class CallbackUtils {
	
	public static interface Callback<T>{
		public void done(final T result, final Throwable throwable);
	}
	
	public static interface ProgressCallback<T> extends Callback<T>{
	    public void onProgress(final T progressStatus);
	    
	    public void onMinorError(final String errorMessage);
	}
	
	public static class VoidCallback implements Callback<Object>{

		@Override
		public final void done(final Object nullable, Throwable throwable) {
			
		}
		
	}
}
