package frontlinesms2

import grails.plugin.mixin.*
import spock.lang.*

@TestFor(SmppFconnection)
class SmppFconnectionSpec extends Specification {
	@Unroll
	def 'SmppFconnection should be saved with proper values'(){
		setup:
			def smppfconnection = new SmppFconnection(username:username, password:password, serverUrl:serverUrl, serverPort:serverPort)
		expect:
			smppfconnection.validate() == valid
		where:
			username|password|serverUrl|serverPort|valid
			'user'|'passwad'|'12.23.34.345'|'9090'|true
			//password
			'user'|''|'12.23.34.345'|'9090'|true
			'user'|null|'12.23.34.345'|'9090'|true
			//username
			''|'passwad'|'12.23.34.345'|'9090'|false
			null|'passwad'|'12.23.34.345'|'9090'|false
			//serverUrl
			'user'|'passwad'|''|'9090'|false
			'user'|'passwad'|null|'9090'|false
			//port
			'user'|'passwad'|'12.23.34.345'|''|false
			'user'|'passwad'|'12.23.34.345'|null|false
	}
}