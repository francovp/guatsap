package chat.manager;

import jade.MicroBoot;
import jade.core.MicroRuntime;
import jade.core.Agent;
import jade.util.leap.Properties;
import jade.wrapper.AgentController;

import java.awt.*;
import java.awt.event.*;

public class StartManager extends MicroBoot {
	  public static void main(String args[]) {
	  	MicroBoot.main(args);
	  	try {
	  		MicroRuntime.startAgent("manager", "chat.manager.ChatManagerAgent", null);
			MicroRuntime.startAgent("manager.mobile", "chat.mobile.MobileAgent", null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  }
}
