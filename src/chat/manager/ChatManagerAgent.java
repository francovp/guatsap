
package chat.manager;


import jade.core.Agent;
import jade.core.AID;

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
import jade.domain.FIPAAgentManagement.FailureException;

import jade.domain.introspection.IntrospectionOntology;
import jade.domain.introspection.Event;
import jade.domain.introspection.DeadAgent;
import jade.domain.introspection.AMSSubscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import chat.ontology.*;

/**
   este agente mantiene conocimiento sobre los agentes que estén en el chat
   e informa cuando alguien se conecta o desconecta
 */
public class ChatManagerAgent extends Agent implements SubscriptionManager {
	private Map<AID, Subscription> participants = new HashMap<AID, Subscription>();
	private Codec codec = new SLCodec();
	private Ontology onto = ChatOntology.getInstance();
	private AMSSubscriber myAMSSubscriber;

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
	}

	protected void takeDown() {
		// Desuscribirse del AMS
		
		send(myAMSSubscriber.getCancel());
		
		//Arreglo: Debe informar de participantes actuales (De haberlos)
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
}
