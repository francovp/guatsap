package chat.client.agent;

import jade.content.ContentManager;
import jade.content.abs.AbsAggregate;
import jade.content.abs.AbsConcept;
import jade.content.abs.AbsPredicate;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BasicOntology;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import jade.util.leap.Iterator;
import jade.util.leap.Set;
import jade.util.leap.SortedSetImpl;
import chat.client.ChatGui;

import chat.client.AWTChatGui;

import chat.ontology.ChatOntology;

/**
 * Este agente implementa la logica del chat cliente que esta ejecutandose en el terminal del usuario.
 * Las interacciones del usuario están manejadas por el "ChatGui" de una manera dependiente. Esta clase
 * utiliza 3 comportamientos:
 * 	- ParticipantsManager. Un CyclicBehaviour que mantiene la lista de participantes actualizada
 * 	basada en la información recibida desde ChatManagerAgent. También está a cargo de suscribir a los
 * 	participantes al ChatManagerAgent.
 * 	- ChatListener. Un CyclicBehaviour que maneja los mensajes desde los chat de los otros participantes.
 * 	- ChatSpeaker. Un OneShotBehaviour que envía un mensaje transformado en una sentencia escrita por el
 * 	usuario a los otros participantes del chat
 *
 */
public class ChatClientAgent extends Agent {
	private static final long serialVersionUID = 1594371294421614291L;

	private Logger logger = Logger.getMyLogger(this.getClass().getName());

	private static final String CHAT_ID = "__chat__";
	private static final String CHAT_MANAGER_NAME = "manager";
	public static final int EXIT = 1000;
	public static final int MOVE_EVENT = 1001;
	public static final int STOP_EVENT = 1002;
	public static final int CONTINUE_EVENT = 1003;
	public static final int REFRESH_EVENT = 1004;
	public static final int CLONE_EVENT = 1005;

	private ChatGui myGui;
	private Set participants = new SortedSetImpl();
	private Codec codec = new SLCodec();
	private Ontology onto = ChatOntology.getInstance();
	private ACLMessage spokenMsg;

	protected void setup() {
		// Registramos el lenguaje y la ontología
		ContentManager cm = getContentManager();
		cm.registerLanguage(codec);
		cm.registerOntology(onto);
		cm.setValidationMode(false);

		// Agregamos comportamientos iniciales
		addBehaviour(new ParticipantsManager(this));
		addBehaviour(new ChatListener(this));

		// Inicializamos el mensaje usado para transmitir las sentencias de los usuarios
		spokenMsg = new ACLMessage(ACLMessage.INFORM);
		spokenMsg.setConversationId(CHAT_ID);

		// Activamos el GUI
		myGui = new AWTChatGui(this);
	}

	protected void takeDown() {
		if (myGui != null) {
			myGui.dispose();
		}
	}

	private void notifyParticipantsChanged() {
		myGui.notifyParticipantsChanged(getParticipantNames());
	}

	private void notifySpoken(String speaker, String sentence) {
		myGui.notifySpoken(speaker, sentence);
	}
	
	/**
	 * Clase interna ParticipantsManager. Este comportamiento registra a los participantes del chat
	 * y mantiene una lista de participantes actualizada para gestionar la información recibida por
	 * el agente ChatManager.
	 */
	class ParticipantsManager extends CyclicBehaviour {
		private static final long serialVersionUID = -4845730529175649756L;
		private MessageTemplate template;

		ParticipantsManager(Agent a) {
			super(a);
		}

		public void onStart() {
			// Suscribe a un participante del chat al agente ChatManager
			ACLMessage subscription = new ACLMessage(ACLMessage.SUBSCRIBE);
			subscription.setLanguage(codec.getName());
			subscription.setOntology(onto.getName());
			String convId = "C-" + myAgent.getLocalName();
			subscription.setConversationId(convId);
			subscription.addReceiver(new AID(CHAT_MANAGER_NAME, AID.ISLOCALNAME));
			myAgent.send(subscription);
			// Inicializamos la plantilla usada para recibir notificaciones desde el ChatManagerAgent
			template = MessageTemplate.MatchConversationId(convId);
		}

		public void action() {
			// Recibe información sobre la gente que entra y sale del chat
			ACLMessage msg = myAgent.receive(template);
			if (msg != null) {
				if (msg.getPerformative() == ACLMessage.INFORM) {
					try {
						AbsPredicate p = (AbsPredicate) myAgent.getContentManager().extractAbsContent(msg);
						// Ve si entra un nuevo participante comparando su ontologia
						if (p.getTypeName().equals(ChatOntology.JOINED)) { 
							// Obtiene nuevos particpantes, agregandolos a la lista y 
							// notificando a la GUI
							AbsAggregate agg = (AbsAggregate) p.getAbsTerm(ChatOntology.JOINED_WHO);
							if (agg != null) {
								Iterator it = agg.iterator();
								while (it.hasNext()) {
									AbsConcept c = (AbsConcept) it.next();
									participants.add(BasicOntology.getInstance().toObject(c));
								}
							}
							notifyParticipantsChanged();
						}
						// Ve si sale un nuevo participante comparando su ontologia
						if (p.getTypeName().equals(ChatOntology.LEFT)) {
							// Rescata a los antiguos participantes, los remueve de la lista
							// y notifica a la GUI
							AbsAggregate agg = (AbsAggregate) p.getAbsTerm(ChatOntology.JOINED_WHO);
							if (agg != null) {
								Iterator it = agg.iterator();
								while (it.hasNext()) {
									AbsConcept c = (AbsConcept) it.next();
									participants.remove(BasicOntology.getInstance().toObject(c));
								}
							}
							notifyParticipantsChanged();
						}
					} catch (Exception e) {
						Logger.println(e.toString());
						e.printStackTrace();
					}
				} else {
					handleUnexpected(msg);
				}
			} else {
				block();
			}
		}
	}

	/**
	 * Clase interna ChatListener. Este comportamiento registra a los participantes del chat
	 * y mantiene una lista de participantes actualizada para gestionar la información recibida por
	 * el agente ChatManager.
	 */
	class ChatListener extends CyclicBehaviour {
		private static final long serialVersionUID = 741233963737842521L;
		private MessageTemplate template = MessageTemplate.MatchConversationId(CHAT_ID);

		ChatListener(Agent a) {
			super(a);
		}

		public void action() {
			ACLMessage msg = myAgent.receive(template);
			if (msg != null) {
				if (msg.getPerformative() == ACLMessage.INFORM) {
					notifySpoken(msg.getSender().getLocalName(), msg.getContent());
				} else {
					handleUnexpected(msg);
				}
			} else {
				block();
			}
		}
	}

	/**
	 * Clase interna ChatSpeaker. Informa a otros participantes que alguien habló
	 */
	private class ChatSpeaker extends OneShotBehaviour {
		private static final long serialVersionUID = -1426033904935339194L;
		private String sentence;

		private ChatSpeaker(Agent a, String s) {
			super(a);
			sentence = s;
		}

		public void action() {
			spokenMsg.clearAllReceiver();
			Iterator it = participants.iterator();
			while (it.hasNext()) {
				spokenMsg.addReceiver((AID) it.next());
			}
			spokenMsg.setContent(sentence);
			notifySpoken(myAgent.getLocalName(), sentence);
			send(spokenMsg);
		}
	}

	// ///////////////////////////////////////
	// Métodos llamados por la interfaz
	// ///////////////////////////////////////
	public void handleSpoken(String s) {
		// Agregar un comportamiento a ChatSpeaker que informa a todos los participantes que alguien habló
		addBehaviour(new ChatSpeaker(this, s));
	}
	
	public String[] getParticipantNames() {
		String[] pp = new String[participants.size()];
		Iterator it = participants.iterator();
		int i = 0;
		while (it.hasNext()) {
			AID id = (AID) it.next();
			pp[i++] = id.getLocalName();
		}
		return pp;
	}

	// ///////////////////////////////////////
	// Método de uso privado
	// ///////////////////////////////////////
	private void handleUnexpected(ACLMessage msg) {
		if (logger.isLoggable(Logger.WARNING)) {
			logger.log(Logger.WARNING, "Mensaje inesperado recibido desde " + msg.getSender().getName());
			logger.log(Logger.WARNING, "El contenido es el siguiente: " + msg.getContent());
		}
	}

}
