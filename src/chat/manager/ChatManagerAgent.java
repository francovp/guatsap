
package chat.manager;


import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.BasicOntology;
import jade.content.abs.*;

import jade.proto.SubscriptionResponder;
import jade.proto.SubscriptionResponder.SubscriptionManager;
import jade.proto.SubscriptionResponder.Subscription;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.FailureException;

import jade.domain.introspection.IntrospectionOntology;
import jade.domain.mobility.MobilityOntology;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;
import jade.domain.introspection.Event;
import jade.domain.introspection.DeadAgent;
import jade.domain.introspection.AMSSubscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.HashMap;
import java.util.Iterator;

import chat.ontology.*;

import jade.core.*;

/**
   este agente mantiene conocimiento sobre los agentes que estén en el chat
   e informa cuando alguien se conecta o desconecta
 */
public class ChatManagerAgent extends GuiAgent implements SubscriptionManager{
	private Map<AID, Subscription> participants = new HashMap<AID, Subscription>();
	private Codec codec = new SLCodec();
	private Ontology onto = ChatOntology.getInstance();
	private AMSSubscriber myAMSSubscriber;
	
	int     cnt;   // this is the counter
	  public boolean cntEnabled;  // this flag indicates if counting is enabled
	  public static MobileAgentGui gui;  // this is the gui
	  Location nextSite;  // this variable holds the destination site
	  
	  // These constants are used by the Gui to post Events to the Agent
	  public static final int EXIT = 1000;
	  public static final int MOVE_EVENT = 1001;
	  public static final int STOP_EVENT = 1002;
	  public static final int CONTINUE_EVENT = 1003;
	  public static final int REFRESH_EVENT = 1004;
	  public static final int CLONE_EVENT = 1005;
	
	// this vector contains the list of visited locations
	  Vector visitedLocations = new Vector();

	protected void setup() {
		//Se prepara para aceptar suscripciones de participantes del chat
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(onto);

		MessageTemplate sTemplate = MessageTemplate.and(
				MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE),
				MessageTemplate.and(
						MessageTemplate.MatchLanguage(codec.getName()),
						MessageTemplate.MatchOntology(onto.getName()) ) );
		addBehaviour(new SubscriptionResponder(this, sTemplate, this));

		// se registra al AMS para detectar cuando un participante muere repentinamente
		myAMSSubscriber = new AMSSubscriber() {
			protected void installHandlers(Map handlersTable) {
				
				// Llena la tabla de adm de eventos. Solo estamos interesados en los eventos DEADAGENT
				
				handlersTable.put(IntrospectionOntology.DEADAGENT, new EventHandler() {
					public void handle(Event ev) {
						DeadAgent da = (DeadAgent)ev;
						AID id = da.getAgent();
						
						//Si el agente estaba en el chat ---> notificar a todos
						//los participantes que se acaba de retirar
						
						if (participants.containsKey(id)) {
							try {
								deregister((Subscription) participants.get(id));
							}
							catch (Exception e) {
								//Nunca debe ocurrir
								e.printStackTrace();
							}
						}
					}
				});
			}
		};
		addBehaviour(myAMSSubscriber);
		
		// register the SL0 content language
		  getContentManager().registerLanguage(new SLCodec(), FIPANames.ContentLanguage.FIPA_SL0);
		  // register the mobility ontology
		  getContentManager().registerOntology(MobilityOntology.getInstance());

		  // creates and shows the GUI
		  this.gui = new MobileAgentGui(this);
		  this.gui.setVisible(true); 

		  // get the list of available locations and show it in the GUI
		  addBehaviour(new GetAvailableLocationsBehaviour(this));

		  // initialize the counter and the flag
		  cnt = 0;
		  cntEnabled = true;

		  ///////////////////////
		  // Add agent behaviours to increment the counter and serve
		  // incoming messages
		  Behaviour b1 = new CounterBehaviour(this);
		  addBehaviour(b1);	
		  Behaviour b2 = new ServeIncomingMessagesBehaviour(this);
		  addBehaviour(b2);	
		
	}

	/////////////////////////////////////////////////////
	// Implementación de la interfaz SubscriptionManager
	/////////////////////////////////////////////////////
	public boolean register(Subscription s) throws RefuseException, NotUnderstoodException { 
		try {
			AID newId = s.getMessage().getSender();
			
			//Notifica a los nuevos participantes sobre los otros (de haberlos) 
			
			if (!participants.isEmpty()) {
				// Mensaje al nuevo participante
				ACLMessage notif1 = s.getMessage().createReply();
				notif1.setPerformative(ACLMessage.INFORM);
				
				//Mensaje para participantes antiguos
				//Notar que el mensaje es el mismo para todos los receptores (una parte desde el
				//conversation id que sera automaticamente ajustada por el Subscription.notify())
				//prepararlo solo una vez fuera del loop :P
				
				ACLMessage notif2 = (ACLMessage) notif1.clone();
				notif2.clearAllReceiver();
				Joined joined = new Joined();
				List<AID> who = new ArrayList<AID>(1);
				who.add(newId);
				joined.setWho(who);
				getContentManager().fillContent(notif2, joined);

				who.clear();
				Iterator<AID> it = participants.keySet().iterator();
				while (it.hasNext()) {
					AID oldId = it.next();
					
					// Notificacion al participante antiguo
					Subscription oldS = (Subscription) participants.get(oldId);
					oldS.notify(notif2);
					
					who.add(oldId);
				}

				// Notificar nuevo participante
				getContentManager().fillContent(notif1, joined);
				s.notify(notif1);
			}
			
			// Agregar la suscripcion
			participants.put(newId, s);
			return false;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RefuseException("Subscription error");
		}		
	}

	public boolean deregister(Subscription s) throws FailureException {
		AID oldId = s.getMessage().getSender();
		
		// Remover la suscripcion
		
		if (participants.remove(oldId) != null) {
			
			// Notificar a otros participantes (De haberlos)
			
			if (!participants.isEmpty()) {
				try {
					ACLMessage notif = s.getMessage().createReply();
					notif.setPerformative(ACLMessage.INFORM);
					notif.clearAllReceiver();
					AbsPredicate p = new AbsPredicate(ChatOntology.LEFT);
					AbsAggregate agg = new AbsAggregate(BasicOntology.SEQUENCE);
					agg.add((AbsTerm) BasicOntology.getInstance().fromObject(oldId));
					p.set(ChatOntology.LEFT_WHO, agg);
					getContentManager().fillContent(notif, p);

					Iterator it = participants.values().iterator();
					while (it.hasNext()) {
						Subscription s1 = (Subscription) it.next();
						s1.notify(notif);
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}
	
	public static MobileAgentGui getGui(){
  		return gui;
  	}
  	
	public void takeDown() {
	  if (gui!=null) {
            gui.dispose();
	    gui.setVisible(false);
	  }
          System.out.println(getLocalName()+" is now shutting down.");
	}

  /**
   * This method stops the counter by disabling the flag
   */
   void stopCounter(){
    cntEnabled = false;
   }

  /**
   * This method resume counting by enabling the flag
   */
   void continueCounter(){
     cntEnabled = true;
   }

  /**
   * This method displays the counter in the GUI
   */
   void displayCounter(){
     gui.displayCounter(cnt);
   }
  
   
protected void beforeClone() {
  System.out.println(getLocalName()+" is now cloning itself.");
}

protected void afterClone() {
  System.out.println(getLocalName()+" has cloned itself.");
  afterMove();
}
  /**
   * This method is executed just before moving the agent to another
   * location. It is automatically called by the JADE framework.
   * It disposes the GUI and prints a bye message on the standard output.
   */
	protected void beforeMove() 
	{
		gui.dispose();
		gui.setVisible(false);
		System.out.println(getLocalName()+" is now moving elsewhere.");
	}

  /**
   * This method is executed as soon as the agent arrives to the new 
   * destination.
   * It creates a new GUI and sets the list of visited locations and
   * the list of available locations (via the behaviour) in the GUI.
   */
   protected void afterMove() {
     System.out.println(getLocalName()+" is just arrived to this location.");
     // creates and shows the GUI
     gui = new MobileAgentGui(this);
     //if the migration is via RMA the variable nextSite can be null.
     if(nextSite != null)
     {
     	visitedLocations.addElement(nextSite);
      for (int i=0; i<visitedLocations.size(); i++)
        gui.addVisitedSite((Location)visitedLocations.elementAt(i));
     }
     gui.setVisible(true); 	
			
     // Register again SL0 content language and JADE mobility ontology,
     // since they don't migrate.
     getContentManager().registerLanguage(new SLCodec(), FIPANames.ContentLanguage.FIPA_SL0);
	 getContentManager().registerOntology(MobilityOntology.getInstance());
     // get the list of available locations from the AMS.
     // FIXME. This list might be stored in the Agent and migrates with it.
     addBehaviour(new GetAvailableLocationsBehaviour(this));
   }

  public void afterLoad() {
      afterClone();
  }

  public void beforeFreeze() {
      beforeMove();
  }

  public void afterThaw() {
      afterMove();
  }

  public void beforeReload() {
      beforeMove();
  }

  public void afterReload() {
      afterMove();
  }


	/////////////////////////////////
	// GUI HANDLING
		

	// AGENT OPERATIONS FOLLOWING GUI EVENTS
	protected void onGuiEvent(GuiEvent ev)
	{
		switch(ev.getType()) 
		{
		case EXIT:
			gui.dispose();
			gui = null;
			doDelete();
			break;
		case MOVE_EVENT:
      Iterator moveParameters = ev.getAllParameter();
      nextSite =(Location)moveParameters.next();
			doMove(nextSite);
			break;
		case CLONE_EVENT:
			Iterator cloneParameters = ev.getAllParameter();
			nextSite =(Location)cloneParameters.next();
			doClone(nextSite,"clone"+cnt+"of"+getName());
			break;
   	case STOP_EVENT:
		  stopCounter();
		  break;
		case CONTINUE_EVENT:
		  continueCounter();
		  break;
		case REFRESH_EVENT:
		  addBehaviour(new GetAvailableLocationsBehaviour(this));
		  break;
		}

	}
}