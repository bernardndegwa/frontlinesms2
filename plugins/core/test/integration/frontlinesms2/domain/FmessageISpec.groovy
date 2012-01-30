package frontlinesms2.domain

import frontlinesms2.*

class FmessageISpec extends grails.plugin.spock.IntegrationSpec {
	
	def 'If any of a Fmessages dispatches has failed its status is HASFAILED'() {
		when:
			def dispatch1 = new Dispatch(dst: '123456', status: DispatchStatus.FAILED)
			def dispatch2 = new Dispatch(dst: '123456', status: DispatchStatus.PENDING)
			def dispatch3 = new Dispatch(dst: '123456', status: DispatchStatus.SENT, dateSent: new Date())
			def message = new Fmessage(date:new Date())
			message.addToDispatches(dispatch1)
			message.addToDispatches(dispatch2)
			message.addToDispatches(dispatch3)
			message.save(failOnError: true)
			message.refresh()
		then:
			message.hasFailed == true
	}
	
	def 'If any of a Fmessages dispatches are pending, but none have failed its status is HASPENDING'() {
		when:
			def dispatch1 = new Dispatch(dst: '123456', status: DispatchStatus.PENDING)
			def dispatch2 = new Dispatch(dst: '123456', status: DispatchStatus.PENDING)
			def dispatch3 = new Dispatch(dst: '123456', status: DispatchStatus.SENT, dateSent: new Date())
			def message = new Fmessage(date:new Date())
			message.addToDispatches(dispatch1)
			message.addToDispatches(dispatch2)
			message.addToDispatches(dispatch3)
			message.save(failOnError: true)
			message.refresh()
		then:
			message.hasPending == true
	}
	
	def 'If all of a Fmessages dispatches have sent is status is HASSENT'() {
		when:
			def dispatch1 = new Dispatch(dst: '123456', status: DispatchStatus.SENT, dateSent: new Date())
			def dispatch2 = new Dispatch(dst: '123456', status: DispatchStatus.SENT, dateSent: new Date())
			def dispatch3 = new Dispatch(dst: '123456', status: DispatchStatus.SENT, dateSent: new Date())
			def message = new Fmessage(date:new Date())
			message.addToDispatches(dispatch1)
			message.addToDispatches(dispatch2)
			message.addToDispatches(dispatch3)
			message.save(failOnError: true)
			message.refresh()
		then:
			message.hasSent
	}
	
	def 'get deleted messages gets all messages with deleted flag'() {
		setup:
				(1..3).each {
				    new Fmessage(isDeleted:true,src:"+123456789", date: new Date(), inbound:true).save(flush:true, failOnError:true)
				}
				(1..2).each {
					new Fmessage(isDeleted:false,src:"+987654321", date: new Date(), inbound:true).save(flush:true, failOnError:true)
				}
		when:
			def deletedMessages = Fmessage.deleted(false)
		then:
			deletedMessages.count() == 3
	}
	
	def "countAllMessages returns all message counts"() {
		setup:
			setUpMessages()
		when:
			def messageCounts = Fmessage.countAllMessages(['archived':false, 'starred': false])
		then:
			messageCounts['inbox'] == 2
			messageCounts['sent'] == 3
			messageCounts['pending'] == 2
			messageCounts['deleted'] == 1
	}
	
	def "countUnreadMessages returns unread messages count"() {
		setup:
			new Fmessage(src: '1234', inbound:true, isDeleted:false, text:'A read message', read:true, date: new Date()).save(flush:true)
			new Fmessage(src: '1234', inbound:true, isDeleted:false, text:'An unread message', read:false, date: new Date()).save(flush:true)
			new Fmessage(src: '1234', inbound:true, isDeleted:false, text:'An unread message', read:false, archived: true, date: new Date()).save(flush:true)
		when:
			def unreadMessageCount = Fmessage.countUnreadMessages()
		then:
			unreadMessageCount == 1
	}

	def "search returns results with a given max and offset"() {
		setup:
			setUpMessages()
			def search = new Search(searchString: 'inbox')
		when:
			def firstInboxMessage = Fmessage.search(search).list(max: 1,  offset:0)
			def firstTwoInboxMessages = Fmessage.search(search).list(max: 2, offset: 0)
			def allMsgsWithTheGivenSearchString = Fmessage.search(search).list()
		then:
			firstInboxMessage.size() == 1
			firstTwoInboxMessages.size() == 2
			allMsgsWithTheGivenSearchString.size() == 3
			Fmessage.search(search).count() == 3
	}

	def "messages are fetched based on message status"() {
		setup:
			setUpMessages()
			def search = new Search(status: ['INBOUND'])
			def search2 = new Search(status: ['SENT', 'PENDING', 'FAILED'])
			def search3 = new Search(searchString: "")
		when:
			def allInboundMessages = Fmessage.search(search).list()
			def allSentMessages = Fmessage.search(search2).list()
			def allMessages = Fmessage.search(search3).list()
		then:
			allInboundMessages*.every { it.inbound }
			allSentMessages*.every { !it.inbound }
			allMessages.size() == 7

	}
	
	def "searching for a group with no members does not throw an error"() {
		setup:
			def footballGroup = new Group(name: "football").save(flush: true)
			def search = new Search(group: footballGroup)
		when:
			def searchMessages = Fmessage.search(search).list()
			def searchMessagesCount = Fmessage.search(search).count()
		then:
			!searchMessages
			searchMessagesCount == 0
	}

	def "getMessageStats should return message traffic information for the filter criteria"() {
		setup:
			final Date TEST_DATE = new Date()
		
			def fsharp = createGroup('Fsharp')
			def haskell = createGroup('Haskell')
			
			def jessy = createContact("Jessy", "+8139878055")
			jessy.addToGroups(fsharp)
			
			def lucy = createContact("Lucy", "+8139878056")
			lucy.addToGroups(haskell)
			
			(TEST_DATE-6..TEST_DATE+5).each { date ->
				new Fmessage(date:date, src:jessy.primaryMobile, inbound:true,
								text:"A message received on $date")
						.save(failOnError:true, flush:true)
				new Fmessage(date:date, src:'1234567890', hasSent:true,
								text:"A message sent on $date")
						// this dispatch should be counted because Jessy is in the target group
						.addToDispatches(new Dispatch(dst:jessy.primaryMobile, status:DispatchStatus.SENT, dateSent:date))
						.save(failOnError:true, flush:true)
			}
			
			3.times {
				new Fmessage(date:TEST_DATE-1, src:jessy.primaryMobile, inbound:true, text: "Message {it}")
						.save(failOnError:true, flush:true)
			}
			
			5.times {
				new Fmessage(date:TEST_DATE, src:'0000000', hasSent:true, text:"Message {it}")
						// this dispatch should be counted because Jessy is in the target group
						.addToDispatches(new Dispatch(dst:jessy.primaryMobile, status:DispatchStatus.SENT, dateSent:TEST_DATE))
						// this dispatch should be ignored because Lucy is not in the target group
						.addToDispatches(new Dispatch(dst:lucy.primaryMobile, status:DispatchStatus.SENT, dateSent:TEST_DATE))
						.save(failOnError:true, flush:true)
			}

		when:
			def asKey = { date -> date.format('dd/MM') }
			def stats = Fmessage.getMessageStats([groupInstance:fsharp, messageOwner:null,
					startDate:TEST_DATE-2, endDate:TEST_DATE])
		then:
			stats.keySet().sort() == [TEST_DATE-2, TEST_DATE-1, TEST_DATE].collect { asKey it }
			stats["${asKey TEST_DATE-1}"] == [sent:1, received:4]
			stats["${asKey TEST_DATE}"] == [sent:6, received:1]
	}

	def "new messages displayName are automatically given the matching contacts name"() {
		setup:
			new Contact(name: "Alice", primaryMobile:"1234", secondaryMobile: "4321").save(flush: true)
		when:
			def alice = Contact.findByName('Alice')
			def dispatch1 = new Dispatch(dst:"1234", status: DispatchStatus.SENT, dateSent: new Date())
			def dispatch2 = new Dispatch(dst:"4321", status: DispatchStatus.SENT, dateSent: new Date())
			def messageFromPrimaryNumber = new Fmessage(src:"1234", inbound:true, date: new Date())
			def messageFromSecondaryNumber = new Fmessage(src:"4321", inbound:true, date: new Date())
			def outBoundMessageToPrimaryNo = new Fmessage(hasSent:true, date: new Date(), text:"").addToDispatches(dispatch1)
			def outBoundMessageToSecondayNo = new Fmessage(hasSent:true, date: new Date(), text:"").addToDispatches(dispatch2)
			messageFromPrimaryNumber.save(flush: true, failOnError:true)
			messageFromSecondaryNumber.save(flush: true, failOnError:true)
			outBoundMessageToPrimaryNo.save(flush: true, failOnError:true)
			outBoundMessageToSecondayNo.save(flush: true, failOnError:true)
		then:
			messageFromPrimaryNumber.displayName == "Alice"
			messageFromSecondaryNumber.displayName == "Alice"
			outBoundMessageToPrimaryNo.displayName == "Alice"
			outBoundMessageToSecondayNo.displayName == "Alice"
	}
	
	def "cannot archive a message that has an owner" () {
		setup:
			def f = new Folder(name:'test').save(failOnError:true)
			def m = new Fmessage(src:'+123456789', text:'hello', date: new Date(), inbound: true).save(failOnError:true)
			f.addToMessages(m)
			f.save()
		when:
			m.archived = true
		then:
			!m.validate()
	}
	
	def "when a new contact is created, all messages with that contacts primary number should be updated"() {
		when:
			def message = new Fmessage(src:'1', inbound:true, date:new Date()).save(flush: true, failOnError:true)
		then:
			message.displayName == '1'
			!message.contactExists
		when:
			new Contact(name:"Alice", primaryMobile:'1', secondaryMobile:'2').save(failOnError:true, flush:true)
			message.refresh()
		then:
			message.displayName == "Alice"
			message.contactExists
	}
	
	def "when a contact is updated, all messages with that contacts primary number should be updated"() {
		when:
			def alice = new Contact(name:"Alice", primaryMobile:'1', secondaryMobile:'2').save(failOnError:true, flush:true)
			def message = new Fmessage(src:'1', inbound:true, date:new Date()).save(failOnError:true, flush:true)
		then:
			message.displayName == 'Alice'
			message.contactExists
		when:
			alice.primaryMobile = '3'
			alice.save(failOnError:true, flush:true)
			message.refresh()
		then:
			message.displayName == '1'
			!message.contactExists
	}
	
	def "when a new contact is created, all messages with that contacts secondary number should be updated"() {
		when:
			def message = new Fmessage(src:'2', inbound:true, date:new Date()).save(flush: true, failOnError:true)
		then:
			message.displayName == '2'
			!message.contactExists
		when:
			new Contact(name:"Alice", primaryMobile:'1', secondaryMobile:'2').save(failOnError:true, flush:true)
			message.refresh()
		then:
			message.displayName == "Alice"
			message.contactExists
	}
	
	def "when a contact is updated, all messages with that contacts secondary number should be updated"() {
		when:
			def alice = new Contact(name:"Alice", primaryMobile:'1', secondaryMobile:'2').save(failOnError:true, flush:true)
			def message = new Fmessage(src:'2', inbound:true, date:new Date()).save(failOnError:true, flush:true)
		then:
			message.displayName == 'Alice'
			message.contactExists
		when:
			alice.secondaryMobile = '3'
			alice.save(failOnError:true, flush:true)
			message.refresh()
		then:
			message.displayName == '2'
			!message.contactExists
	}
	
	def "when a contact is updated and primary number is moved to secondary number messages from that number should still appear from that contact"() {
		when:
			def alice = new Contact(name:"Alice", primaryMobile:'1', secondaryMobile:'2').save(failOnError:true, flush:true)
			def message = new Fmessage(src:'1', inbound:true, date:new Date()).save(failOnError:true, flush:true)
		then:
			message.displayName == 'Alice'
			message.contactExists
		when:
			alice.primaryMobile = '3'
			alice.secondaryMobile = '1'
			alice.save(failOnError:true, flush:true)
			message.refresh()
		then:
			message.displayName == 'Alice'
			message.contactExists
	}
	
	def "can archive message when it has no message owner" () {
		setup:
			def m = new Fmessage(src:'+123456789', text:'hello', date: new Date(), inbound: true).save(failOnError:true)
		when:
			m.archived = true
		then:
			m.validate()
	}
	
	def createGroup(String n) {
		new Group(name: n).save(failOnError: true)
	}

	def createContact(String n, String a) {
		def c = new Contact(name: n, primaryMobile: a)
		c.save(failOnError: true)
	}

	private def setUpMessages() {
			def dispatch = new Dispatch(dst:'+254114433', status: DispatchStatus.SENT, dateSent: new Date())
			def pendingDispatch = new Dispatch(dst:'+254114433', status: DispatchStatus.PENDING)
			def failedDispatch = new Dispatch(dst:'+254114433', status: DispatchStatus.FAILED)
			new Fmessage(src: '1234', inbound:true, isDeleted:false, text:'An inbox message', date: new Date()).save(flush:true, failOnError:true)
			new Fmessage(src: '1234', inbound:true, isDeleted:false, text:'Another inbox message', date: new Date()).save(flush:true, failOnError:true)
			new Fmessage(src: '1234', hasSent:true, isDeleted:false, text:'A sent message', date: new Date()).addToDispatches(dispatch).save(flush:true, failOnError:true)
			new Fmessage(hasSent: true, isDeleted:false, text:'Another sent message', date: new Date()).addToDispatches(dispatch).addToDispatches(dispatch).save(flush:true, failOnError:true)
			new Fmessage(hasSent: true, isDeleted:true, text:'Deleted sent message', date: new Date()).addToDispatches(dispatch).save(flush:true, failOnError:true)
			new Fmessage(hasSent: true, isDeleted:false, text:'This msg will not show up in inbox view', date: new Date()).addToDispatches(dispatch).save(flush:true, failOnError:true)
			new Fmessage(hasFailed: true, isDeleted:false, text:'A sent failed message', date: new Date()).addToDispatches(failedDispatch ).save(flush:true, failOnError:true)
			new Fmessage(hasPending: true, isDeleted:false, text:'A pending message', date: new Date()).addToDispatches(pendingDispatch).save(flush:true, failOnError:true)
	}
}