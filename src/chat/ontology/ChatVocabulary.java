package chat.ontology;

/**
  * Vocabulario que contiene simbolos usados
  * dentro de la aplicaci�n de chat
 */
public interface ChatVocabulary {
	// Nombre de Ontologia
  public static final String ONTOLOGY_NAME = "Chat-ontology";
	
	// Vocabulario
  public static final String JOINED = "joined";
  public static final String JOINED_WHO = "who";

  public static final String LEFT = "left";
  public static final String LEFT_WHO = "who";

  public static final String SPOKEN = "spoken";
  public static final String SPOKEN_WHAT = "what";
}