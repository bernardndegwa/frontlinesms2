package frontlinesms2

import spock.lang.*
import grails.plugin.spock.*

class ContactSpec extends UnitSpec {
	def setup() {
		mockForConstraintsTests(Contact)
	}

	def "contact may have a name"() {
		when:
			Contact c = new Contact()
			assert c.name == null
			c.name = 'Alice'
		then:
			c.name == 'Alice'
	}

	def "blank names are allowed, there is no minimum length for name"() {
		when:
			def noNameContact = new Contact(name:'', address:'9876543')
			def namedContact = new Contact(name:'a')
		then:
			noNameContact.validate()
			namedContact.validate()
	}

	def "duplicate names are allowed"(){
		setup:
			mockDomain(Contact)
		when:
			def Contact contact1 = new Contact(name:'John')
			def Contact contact2 = new Contact(name:'John')
		then:
			contact1.save()
			contact2.save()
	}

	def "max name length 255"(){
		when:
			def Contact contact = new Contact(name:'''\
0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef\
0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef\
0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef\
0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef''')
	then:
		!contact.validate()
	}

  def "should return the count of all messages sent to a given contact"() {
	setup:
		String destinationAddress = "9876543210"
		Contact contact = new Contact(name: "John", address: destinationAddress)
		mockDomain Fmessage,[new Fmessage(dst: destinationAddress, deleted : false),
		new Fmessage(dst: destinationAddress, deleted : true),
		new Fmessage(dst: destinationAddress, deleted : true)]
	    when:
	        def count = contact.inboundMessagesCount()
	    then:
	        assert 3 == count
  }

}

