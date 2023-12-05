/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.client;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.api.client.*;

import java.util.Objects;

public class RestClientManager {
	private final String hostname;

	private final IAuthenticationArtemisClient loginManager;
	private ISubmissionsArtemisClient submissionClient;
	private ICourseArtemisClient courseClient;
	private IUtilArtemisClient utilClient;
	private AssessmentArtemisClient assessmentClient;
	private IExamArtemisClient examClient;

	public RestClientManager(String hostname, String username, String password) {
		this(hostname, new LoginManager(hostname, username, password));
	}

	public RestClientManager(String hostname, LoginManager loginManager) {
		this.hostname = Objects.requireNonNull(hostname).trim();
		this.loginManager = Objects.requireNonNull(loginManager);
	}

	public boolean isReady() {
		return this.loginManager.isLoggedIn();
	}

	public String getArtemisUrl() {
		return this.loginManager.getArtemisUrl();
	}

	public void login() throws ArtemisClientException {
		this.loginManager.login();
	}

	public IAuthenticationArtemisClient getAuthenticationClient() {
		return this.loginManager;
	}

	public ISubmissionsArtemisClient getSubmissionArtemisClient() {
		// Initialize the assessment client first, as it is needed for the submission
		// client
		getAssessmentArtemisClient();

		if (this.submissionClient == null) {
			this.submissionClient = new SubmissionsArtemisClient(this.hostname, this.loginManager.getToken(), this.loginManager.getUser(),
					this.assessmentClient);
		}
		return this.submissionClient;
	}

	public ICourseArtemisClient getCourseArtemisClient() {
		if (this.courseClient == null) {
			this.courseClient = new MappingLoaderArtemisClient(this.getSubmissionArtemisClient(), this.hostname, this.loginManager.getToken());
		}
		return this.courseClient;
	}

	public IUtilArtemisClient getUtilArtemisClient() {
		if (this.utilClient == null) {
			this.utilClient = new UtilArtemisClient(this.hostname);
		}
		return this.utilClient;
	}

	public IAssessmentArtemisClient getAssessmentArtemisClient() {
		if (this.assessmentClient == null) {
			this.assessmentClient = new AssessmentArtemisClient(this.hostname, this.loginManager.getToken());
		}
		return this.assessmentClient;
	}

	public IExamArtemisClient getExamArtemisClient() {
		if (this.examClient == null) {
			this.examClient = new ExamArtemisClient(this.hostname, this.loginManager.getToken());
		}
		return this.examClient;
	}

	public void resetClients() {
		this.submissionClient = null;
		this.courseClient = null;
		this.utilClient = null;
		this.assessmentClient = null;
		this.examClient = null;
	}
}
