package frontlinesms2.domain

import frontlinesms2.*

import spock.lang.*

class PollISpec extends grails.plugin.spock.IntegrationSpec {

	def 'Deleted messages do not show up as responses'() {
		when:
			def message1 = new Fmessage(src:'Bob', text:'I like manchester', inbound:true, date: new Date()).save()
			def message2 = new Fmessage(src:'Alice', text:'go barcelona', inbound:true, date: new Date()).save()
			def p = new Poll(name: 'This is a poll')
			p.editResponses(choiceA: 'Manchester', choiceB:'Barcelona')
			p.save(failOnError:true, flush:true)
			PollResponse.findByValue('Manchester').addToMessages(message1)
			PollResponse.findByValue('Barcelona').addToMessages(message2)
			p.save(flush:true, failOnError:true)
		then:
			p.getActivityMessages().count() == 2
		when:
			message1.isDeleted = true
			message1.save(flush:true, failOnError:true)
		then:
			p.getActivityMessages().count() == 1
	}

	def 'Response stats are calculated correctly, even when messages are deleted'() {
		given:
			def p = new Poll(name: 'Who is badder?')
			p.editResponses(choiceA:'Michael-Jackson', choiceB:'Chuck-Norris')
			p.save(failOnError:true, flush:true)
		when:
			def ukId = PollResponse.findByValue('Unknown').id
			def mjId = PollResponse.findByValue('Michael-Jackson').id
			def cnId = PollResponse.findByValue('Chuck-Norris').id
		then:
			p.responseStats == [
				[id:mjId, value:"Michael-Jackson", count:0, percent:0],
				[id:cnId, value:"Chuck-Norris", count:0, percent:0],
				[id:ukId, value:"Unknown", count:0, percent:0]
			]
		when:
			PollResponse.findByValue('Michael-Jackson').addToMessages(new Fmessage(text:'MJ', date: new Date(), inbound: true, src: '12345').save(failOnError:true, flush:true))
			PollResponse.findByValue('Chuck-Norris').addToMessages(new Fmessage(text:'big charlie', date: new Date(), inbound: true, src: '12345').save(failOnError:true, flush:true))
		then:
			p.responseStats == [
				[id:mjId, value:'Michael-Jackson', count:1, percent:50],
				[id:cnId, value:"Chuck-Norris", count:1, percent:50],
				[id:ukId, value:'Unknown', count:0, percent:0]
			]
		when:
			Fmessage.findByText('MJ').isDeleted = true
			Fmessage.findByText('MJ').save(flush:true)
		then:
			p.responseStats == [
				[id:mjId, value:'Michael-Jackson', count:0, percent:0],
				[id:cnId, value:'Chuck-Norris', count:1, percent:100],
				[id:ukId, value:'Unknown', count:0, percent:0]
			]
	}

	def "creating a new poll also creates a poll response with value 'Unknown'"() {
		when:
			def p = new Poll(name: 'This is a poll')
			p.editResponses(choiceA: 'one', choiceB:'two')
			p.save(flush: true)
		then:
			p.responses.size() == 3
	}

    def "should sort messages based on date"() {
		setup:
			setUpPollResponseAndItsMessages()
		when:
			def results = Poll.findByName('question').getActivityMessages()
		then:
			results.list(sort:'date', order:'desc')*.src == ["src2", "src3", "src1"]
			results.list().every {it.archived == false}
    }

	def "should fetch starred poll messages"() {
		setup:
			setUpPollResponseAndItsMessages()
		when:
			def results = Poll.findByName("question").getActivityMessages(true)
		then:
			results.list()*.src == ["src3"]
			results.list().every {it.archived == false}
	}

	def "should check for offset and limit while fetching poll messages"() {
		setup:
			setUpPollResponseAndItsMessages()
		when:
			def results = Poll.findByName("question").getActivityMessages().list(max:1, offset:0)
		then:
			results*.src == ["src2"]
	}

	def "should return count of poll messages"() {
		setup:
			setUpPollResponseAndItsMessages()
		when:
			def results = Poll.findByName("question").getActivityMessages().count()
		then:
			results == 3
	}
	
	def "adding responses to a poll with multiple responses does not affect categorized messages"() {
		when:
			def poll = new Poll(name:"title")
			poll.editResponses(choiceA: "one", choiceB: "two")
			poll.save(flush: true)
			def m1 = Fmessage.build(src: "src1", inbound: true, date: new Date() - 10)
			def m2 = Fmessage.build(src: "src2", inbound: true, date: new Date() - 10)
			def m3 = Fmessage.build(src: "src3", inbound: true, date: new Date() - 10)
			PollResponse.findByValue("one").addToMessages(m1)
			PollResponse.findByValue("one").addToMessages(m2)
			PollResponse.findByValue("two").addToMessages(m3)
			poll.save(flush:true)
			poll.refresh()
		then:
			poll.responses*.liveMessageCount == [2, 1, 0]
		when:
			poll.editResponses(choiceC: "three", choiceD:"four")
			poll = Poll.get(poll.id)
		then:
			poll.responses*.value.containsAll(['one', 'two', 'three', 'four', 'Unknown'])
			poll.responses*.liveMessageCount == [2, 1, 0, 0, 0]
		when:
			m1 = Fmessage.build(src: "src1", inbound: true, date: new Date() - 10)
			PollResponse.findByValue("one").addToMessages(m1)
			PollResponse.findByValue("three").addToMessages(Fmessage.build(src: "src4", inbound: true, date: new Date() - 10))
			poll.save(flush:true)
		then:
			poll.responses*.liveMessageCount == [3, 1, 0, 1, 0]
		when:
			poll.editResponses(choiceA: "five")
			poll.save(flush:true)
			poll.refresh()
		then:
			!PollResponse.findByValue("one")
			println "poll responses ${poll.responses*.value}"
			poll.responses*.every {
				(it.key=='Unknown' && it.liveMessageCount == 3) ||
						(it.key == 'choiceA' && it.liveMessageCount == 0)
			}
	}
	
	def "Archiving a poll archives messages associated with the poll"() {
		given:
			def poll = new Poll(name: 'Who is badder?')
			poll.editResponses(choiceA:'Michael-Jackson', choiceB:'Chuck-Norris')
			poll.save(failOnError:true, flush:true)

			def message1 = Fmessage.build(src:'Bob', text:'I like manchester')
			def message2 = Fmessage.build(src:'Alice', text:'go barcelona')
			poll.addToMessages(message1)
			poll.addToMessages(message2)
			poll.save(flush:true, failOnError:true)
		when:
			poll.archive()
			poll.save(flush:true, failOnError:true)
			poll.refresh()
		then:
			poll.liveMessageCount == 2
			poll.activityMessages.list().every { it.archived }
	}
	
	private def setUpPollAndResponses() {		
		def poll = new Poll(name: 'question')
		poll.addToResponses(PollResponse.createUnknown())
		poll.addToResponses(new PollResponse(value:"response 1"))
		poll.addToResponses(new PollResponse(value:"response 2"))
		poll.addToResponses(new PollResponse(value:"response 3"))
		poll.save(flush: true, failOnError:true)
		return poll
	}

	private def setUpPollResponseAndItsMessages() {
		def poll = setUpPollAndResponses()
		def m1 = Fmessage.build(src: "src1", inbound: true, date: new Date() - 10)
		def m2 = Fmessage.build(src: "src2", inbound: true, date: new Date() - 2)
		def m3 = Fmessage.build(src: "src3", inbound: true, date: new Date() - 5, starred: true)
		PollResponse.findByValue("response 1").addToMessages(m1)
		PollResponse.findByValue("response 2").addToMessages(m2)
		PollResponse.findByValue("response 3").addToMessages(m3)
		poll.save(flush:true, failOnError:true)
	}

	def 'Adding a message will propogate it to the Unknown response'() {
		given:
			Poll p = setUpPollAndResponses()
			Fmessage m = Fmessage.build(date:new Date(), inbound:true, src:"a-unit-test!").save(flush:true, failOnError:true)
			p.refresh()
			m.refresh()
			p.responses*.refresh()
			println "p.responses: $p.responses"
			println "p.responses.messages: $p.responses.messages"
		when:
			println "p.responses*.value: ${p.responses*.value}"
			println "p.responses.find { it.value == 'Unknown' }: ${p.responses.find { it.value == 'Unknown' }}"
			p.addToMessages(m)
			p.save(failOnError:true, flush:true)
		then:
			p.refresh()
			m.refresh()
			p.responses*.refresh()
			p.messages*.id == [m.id]
			p.responses.find { it.value == 'Unknown' }.messages*.id == [m.id]
			p.messages*.id == [m.id]
	}

	// TODO move this test to MessageControllerISpec	
	def "Message should not remain in old PollResponse after moving it to inbox"(){
		given:
			def m = Fmessage.build(inbound:true)
			def responseA = new PollResponse(key:'A', value:'TessstA')
			def previousOwner = new Poll(name:'This is a poll', question:'What is your name?')
					.addToResponses(responseA)
					.addToResponses(key:'B' , value:'TessstB')
					.addToResponses(PollResponse.createUnknown())
					.addToMessages(m)
			responseA.addToMessages(m)
			previousOwner.save(flush:true, failOnError:true)

			assert responseA.refresh().messages.contains(m)
			
			// TODO move this test to MessageController
			def controller = new MessageController()
			controller.params.messageId = m.id
			controller.params.ownerId = 'inbox'
			controller.params.messageSection = 'inbox'
		when:
			controller.move()
		then:
			!previousOwner.messages.contains(m)
			!responseA.messages.contains(m)
			!m.messageOwner
	}

	@Unroll
	def "Message should be sorted into the correct PollResponse for  Poll with top level and second level keywords"() {
		when:
			def p = new Poll(name: 'This is a poll', yesNo:false)
			p.addToResponses(new PollResponse(key:'A', value:"Manchester"))
			p.addToResponses(new PollResponse(key:'B', value:"Barcelona"))
			p.addToResponses(new PollResponse(key:'C', value:"Harambee Stars"))
			p.addToResponses(PollResponse.createUnknown())
			p.save(failOnError:true)
			def k1 = new Keyword(value: "FOOTBALL", activity: p)
			def k2 = new Keyword(value: "MANCHESTER", activity: p, ownerDetail:"A", isTopLevel:false)
			def k3 = new Keyword(value: "HARAMBEE", activity: p, ownerDetail:"C", isTopLevel:false)
			def k4 = new Keyword(value: "BARCELONA", activity: p, ownerDetail:"B", isTopLevel:false)
			p.addToKeywords(k1)
			p.addToKeywords(k2)
			p.addToKeywords(k3)
			p.addToKeywords(k4)
			p.save(failOnError:true, flush:true)
		then:
			p.getPollResponse(new Fmessage(src:'Bob', text:"FOOTBALL something", inbound:true, date:new Date()).save(), Keyword.findByValue(keywordValue)).value == pollResponseValue
		where:
			keywordValue|pollResponseValue
			"FOOTBALL"|"Unknown"
			"MANCHESTER"|"Manchester"	
			"BARCELONA"|"Barcelona"
			"HARAMBEE"|"Harambee Stars"
	}

	@Unroll
	def "Message should be sorted into the correct PollResponse for  Poll with only top level keywords"() {
		when:
			def p = new Poll(name: 'This is a poll', yesNo:false)
			p.addToResponses(new PollResponse(key:'A', value:"Manchester"))
			p.addToResponses(new PollResponse(key:'B', value:"Barcelona"))
			p.addToResponses(new PollResponse(key:'C', value:"Harambee Stars"))
			p.addToResponses(PollResponse.createUnknown())
			p.save(failOnError:true)
			def k2 = new Keyword(value: "MANCHESTER", activity: p, ownerDetail:"A", isTopLevel:true)
			def k3 = new Keyword(value: "HARAMBEE", activity: p, ownerDetail:"C", isTopLevel:true)
			def k4 = new Keyword(value: "BARCELONA", activity: p, ownerDetail:"B", isTopLevel:true)
			p.addToKeywords(k2)
			p.addToKeywords(k3)
			p.addToKeywords(k4)
			p.save(failOnError:true, flush:true)
			println "##### ${p.keywords*.value}"
		then:
			p.getPollResponse(new Fmessage(src:'Bob', text:"FOOTBALL something", inbound:true, date:new Date()).save(), p.keywords.find{ it.value == keywordValue }).value == pollResponseValue
		where:
			keywordValue|pollResponseValue
			"MANCHESTER"|"Manchester"
			"BARCELONA"|"Barcelona"
			"HARAMBEE"|"Harambee Stars"
	}

	def "saving a poll with a response value empty should fail"(){
		given:
			def p = new Poll(name: 'My Team poll')
			p.editResponses(choiceA: 'Manchester', choiceB:'Barcelona', aliasA: 'A,manu,yeah',aliasB: 'B,barca,bfc')
			p.save(failOnError:true)
			def controller = new PollController()
			controller.params.ownerId = p.id
			controller.params.choiceA = "My team"
			controller.params.choiceB = ""
		when:
			controller.save()
		then:
			p.refresh()
			p.responses*.value.containsAll(["Manchester", "Barcelona", "Unknown"])
	}
}
