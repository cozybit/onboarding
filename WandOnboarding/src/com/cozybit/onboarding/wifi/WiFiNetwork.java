package com.cozybit.onboarding.wifi;

/* Small inner class to Store and transfer WiFiNetwork without all other clutter */
public class WiFiNetwork {

	public int networkId = -1;
	public String SSID;
	public String authentication;
	public String password;
	
	public WiFiNetwork(String SSID, String authentication, String password) {
		this.SSID = SSID;
		this.authentication = authentication;
		this.password = password;
	}

	public boolean isSameConfig(String SSID, String authentication,
			String password) {

		 if (SSID == null || authentication == null)
			 return false;

		 if (!this.SSID.equals(SSID))
			 return false;

		 if (!this.authentication.equals(authentication))
			 return false;
 
		 // If authentication is NOT open
		 if (!this.authentication.equals("OPEN")) {
			 // If the autentication is NOT open but password is null
			 if (password == null)
				 return false;
			 // If password is the same we
			 if (!this.password.equals(password))
				 return false;
		 }
		 // Data matches!
		 return true;
	}
};
