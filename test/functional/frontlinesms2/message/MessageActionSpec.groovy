package frontlinesms2.message

import frontlinesms2.*

class MessageActionSpec extends frontlinesms2.poll.PollGebSpec {
	def 'message actions menu is displayed for all individual messages'() {
		given:
			createTestPolls()
			createTestMessages()
		when:
			to PollMessageViewPage
			def actions = $('#message-actions li').children('a')*.text()
		then:
			actions[0] == 'Shampoo Brands'

		when:
			go "message/inbox/show/${Fmessage.findBySrc("Bob").id}"
			def inboxActions = $('#message-actions li a')*.text()
		then:
			inboxActions[0] == 'Football Teams'
		cleanup:
			deleteTestPolls()
			deleteTestMessages()

	}
	
	def 'clicking on poll moves the message to that poll and removes it from the previous poll or inbox'() {
		given:
			createTestPolls()
			createTestMessages()
		when:
			to PollMessageViewPage
			def btnAction = $('#message-actions li').children('a').first()
			def bob = Fmessage.findBySrc('Bob')
			def jill = Fmessage.findBySrc('Jill')
			def shampooPoll = Poll.findByTitle('Shampoo Brands')
			def footballPoll = Poll.findByTitle('Football Teams')
			btnAction.click()
			shampooPoll.responses.each{ it.refresh() }
			footballPoll.responses.each{ it.refresh() }
		then:
			bob != Poll.findByTitle("Football Teams").getMessages().find { it == bob }
			bob == Poll.findByTitle("Shampoo Brands").getMessages().find { it == bob }

		when:
			go "message/inbox/show/${jill.id}"
			btnAction = $('#message-actions li').children('a').first()
			btnAction.click()
			footballPoll.responses.each { it.refresh() }
			Fmessage.findAll().each { it.refresh() }
		then:
			jill != Fmessage.getInboxMessages().find { it == jill }
			jill == Poll.findByTitle("Football Teams").getMessages().find { it == jill }
		cleanup:
			deleteTestPolls()
			deleteTestMessages()
	}

	def 'messages are always added to the "unknown" response of a poll'() {
		given:
			createTestPolls()
			createTestMessages()
		when:
			to PollMessageViewPage
			def btnAction = $('#message-actions li').children('a').first()
			def bob = Fmessage.findBySrc('Bob')
			def jill = Fmessage.findBySrc('Jill')
			def shampooPoll = Poll.findByTitle('Shampoo Brands')
			def footballPoll = Poll.findByTitle('Football Teams')
			btnAction.click()
			shampooPoll.responses.each{ it.refresh() }
			footballPoll.responses.each{ it.refresh() }
			def response =  Poll.findByTitle("Shampoo Brands").getResponses().find { it.value == 'Unknown'}
		then:
			bob.messageOwner == response
		cleanup:
			deleteTestPolls()
			deleteTestMessages()
	}

	def 'possible poll responses are shown in action list and are clickable'() {
		given:
			createTestPolls()
			createTestMessages()
		when:
			to PollMessageViewPage
			def footballPoll = Poll.findByTitle('Football Teams')
			def bob = Fmessage.findBySrc('Bob')
			def btnBarce = $('#poll-actions li:nth-child(2) a')
			btnBarce.click()
			footballPoll.responses.each{ it.refresh() }
			def barceResponse =  footballPoll.getResponses().find { it.value == 'barcelona'}
		then:
			bob.messageOwner == barceResponse
		cleanup:
			deleteTestPolls()
			deleteTestMessages()
	}
	
	def 'existing folders appear in message actions menu'() {
		given:
			createTestFolders()
			new Fmessage(src:'Max', dst:'+254987654', text:'I will be late', inbound: true).save(failOnError:true, flush:true)
		when:
			go 'message'
			def actions = $('#message-actions li').children('a')*.text()
		then:
			actions[0] == 'Work'

		when:
			go "message/inbox/show/${Fmessage.findBySrc("Max").id}"
			def inboxActions = $('#message-actions li a')*.text()
		then:
			inboxActions[0] == 'Work'
		cleanup:
			deleteTestFolders()
			deleteTestMessages()
	}
	
		def 'clicking on folder moves the message to that folder and removes it from the previous location'() {
		given:
			createTestFolders()
			createTestPolls()
			createTestMessages()
			new Fmessage(src:'Max', dst:'+254987654', text:'I will be late', inbound: true).save(failOnError:true, flush:true)
		when:
			go "message/folder/${Folder.findByValue('Work').id}/show/${Fmessage.findBySrc('Max').id}"
			def btnAction = $('#message-actions li').children('a').first()
			def max = Fmessage.findBySrc('Max')
			def jill = Fmessage.findBySrc('Jill')
			def shampooPoll = Poll.findByTitle('Shampoo Brands')
			def footballPoll = Poll.findByTitle('Football Teams')
			btnAction.click()
			shampooPoll.responses.each{ it.refresh() }
			footballPoll.responses.each{ it.refresh() }
		then:
			max != Folder.findByValue("Work").getMessages().find { it == max }
			max == Poll.findByTitle("Shampoo Brands").getMessages().find { it == max }

		when:
			go "message/inbox/show/${jill.id}"
			btnAction = $('#message-actions li').children('a').first()
			btnAction.click()
			footballPoll.responses.each { it.refresh() }
			Fmessage.findAll().each { it.refresh() }
		then:
			jill != Fmessage.getInboxMessages().find { it == jill }
			jill == Poll.findByTitle("Football Teams").getMessages().find { it == jill }
		cleanup:
			deleteTestPolls()
			deleteTestMessages()
	}
	
}
class PollMessageViewPage extends geb.Page {
 	static getUrl() { "message/poll/${Poll.findByTitle('Football Teams').id}/show/${Fmessage.findBySrc("Bob").id}" }
}
