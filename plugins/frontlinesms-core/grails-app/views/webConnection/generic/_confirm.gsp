<%@ page import="frontlinesms2.GenericWebConnection" %>
<div class="input">
	<label for="name"><g:message code="webconnection.name.prompt"/></label>
	<g:textField name="name" value="${activityInstanceToEdit?.name}" class="required"/>
</div>
<div class="confirm">
	<h2><g:message code="webconnection.details.label"/></h2>
	<fsms:activityConfirmTable fields="httpMethod, url, keyword, parameters" type="${GenericWebConnection.type}" instanceClass="${GenericWebConnection}"/>
</div>

