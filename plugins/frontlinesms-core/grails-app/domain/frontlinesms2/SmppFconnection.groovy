package frontlinesms2

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.RouteDefinition


class SmppFconnection extends Fconnection {
	static passwords = ['password']
	static configFields = ['username', 'serverUrl', 'serverPort', 'sendEnabled', 'receiveEnabled']
	static defaultValues = [send:true, receive:true]
	static String getShortName() { 'smpp' }

	String username
	String password
	String serverUrl
	String serverPort
	boolean sendEnabled = true
	boolean receiveEnabled = true

	List<RouteDefinition> getRouteDefinitions() { return null }
}