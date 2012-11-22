package frontlinesms2

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.RouteDefinition
import frontlinesms2.camel.smpp.*


class SmppFconnection extends Fconnection {
	static passwords = ['password']
	static configFields = ['name','username', 'serverUrl', 'serverPort', 'sendEnabled', 'receiveEnabled']
	static defaultValues = [sendEnabled:true, receiveEnabled:true]
	static String getShortName() { 'smpp' }

	String name
	String username
	String password
	String serverUrl
	String serverPort
	boolean sendEnabled = true
	boolean receiveEnabled = true

	static mapping = {
		password column: 'smpp_password'
	}

	List<RouteDefinition> getRouteDefinitions() {
		return new RouteBuilder() {
			@Override void configure() {}
			List getRouteDefinitions() {
				return [from("seda:out-${SmppFconnection.this.id}")
						.onException(Exception)
									.handled(true)
									.beanRef('fconnectionService', 'handleDisconnection')
									.end()
						.setHeader(Fconnection.HEADER_FCONNECTION_ID, simple(SmppFconnection.this.id.toString()))
						.process(new SmppPreProcessor())
						.to("smpp://${username}@${serverUrl}:${serverPort}?password=${password}&enquireLinkTimer=3000&transactionTimer=5000&systemType=producer")
						.process(new SmppPostProcessor())
						.routeId("out-internet-${SmppFconnection.this.id}")]
			}
		}.routeDefinitions
	}
}