package Usage;

import Scheduler.Action;

@SuppressWarnings({"unused"})
public class Recurrence extends Action<Boolean, Object> {

	private String text;
	private String subject;
	private String[] addressees;

	public Recurrence(String name, long progress, String[] addressees, String subject, String text) {
		super(name, progress);

		this.addressees = addressees;
		this.subject = subject;
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String[] getAddressees() {
		return addressees;
	}

	public void setAddressees(String[] addressees) {
		this.addressees = addressees;
	}

	@Override
	protected Object infoProcessing(Object info) {
		//nothing, in this example there is only one action
		//so there is'nt any processing

		appendLog("InfoProcessing\n");
		return null;
	}

	@Override
	protected Boolean exe(Object info) {
		//try to send mails with subject and text

		appendLog("Send mail with subject " + subject);
		return true;

		//logs.append("Error sending mails");
		//catch exceptions
		//return false;
	}
}
