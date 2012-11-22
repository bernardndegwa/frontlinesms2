package frontlinesms2.camel.smpp

import frontlinesms2.*
import org.apache.camel.*

class SmppPostProcessor {
	public void process(Exchange exchange) throws Exception {
		def log = { println "SmppPostProcessor.process() : $it" }
		log 'ENTRY'
		log "in.body:" + exchange.in.body
		byte[] bytes = exchange.in.getBody(byte[].class);
		log "in.body as byte[]:" + bytes
		String text = new String(bytes, "UTF-8").trim();
		log "in.body as byte[] as String:" + text
		log "in.body got as a string" + exchange.in.getBody(String.class)
		log 'EXIT'
	}
}