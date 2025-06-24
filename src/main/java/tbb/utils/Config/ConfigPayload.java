// Name: Michael Amyotte
// Date: 6/13/25
// Purpose: Configurator 'payload' object to marshal in and out of JSON form

package tbb.utils.Config;

public class ConfigPayload {
	// your configuration parameters
	public boolean headless;
	
	
	public ConfigPayload() {
		this(false);
	}
	
	public ConfigPayload(boolean headless) {
		this.headless = headless;
	}
}
