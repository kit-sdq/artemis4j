/* Licensed under EPL-2.0 2023. */
package edu.kit.kastel.sdq.artemis4j.client;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.api.artemis.Course;
import edu.kit.kastel.sdq.artemis4j.api.artemis.exam.Exam;
import edu.kit.kastel.sdq.artemis4j.api.artemis.exam.StudentExam;
import edu.kit.kastel.sdq.artemis4j.api.client.IExamArtemisClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExamArtemisClient extends AbstractArtemisClient implements IExamArtemisClient {

	private static final Logger log = LoggerFactory.getLogger(ExamArtemisClient.class);
	private static final Object STUDENT_EXAMS_PATHPART = "student-exams";

	private final OkHttpClient client;

	public ExamArtemisClient(String hostname, String token) {
		super(hostname);
		this.client = this.createClient(token);
	}

	@Override
	public SubmitResult markAllExamsAsSubmitted(Course course, Exam exam) throws ArtemisClientException {
		List<StudentExam> studentExams = getStudentExams(course, exam);
		List<StudentExam> toggleSuccess = new ArrayList<>();
		List<StudentExam> toggleFailed = new ArrayList<>();

		for (StudentExam studentExam : studentExams) {
			if (studentExam.isSubmitted())
				continue;

			boolean toggled = toggleToSubmitted(course, exam, studentExam);
			if (toggled)
				toggleSuccess.add(studentExam);
			else
				toggleFailed.add(studentExam);

		}
		return new SubmitResult(studentExams, toggleSuccess, toggleFailed);
	}

	private boolean toggleToSubmitted(Course course, Exam exam, StudentExam studentExam) {
		try {
			log.info("Toggling student exam {} to submitted (User: {})", studentExam.getId(), studentExam.getStudent().getLogin());
			Request request = new Request.Builder() //
					.url(this.path(COURSES_PATHPART, course.getCourseId(), EXAMS_PATHPART, exam.getExamId(), STUDENT_EXAMS_PATHPART, studentExam.getId(),
							"toggle-to-submitted"))
					.put(RequestBody.create("{}", JSON)).build();
			this.call(this.client, request, null);
			return true;
		} catch (ArtemisClientException e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public List<StudentExam> getStudentExams(Course course, Exam exam) throws ArtemisClientException {
		log.debug("Requesting student exams for exam {}", exam.getExamId());

		Request request = new Request.Builder() //
				.url(this.path(COURSES_PATHPART, course.getCourseId(), EXAMS_PATHPART, exam.getExamId(), STUDENT_EXAMS_PATHPART)).get().build();

		StudentExam[] studentExams = this.call(this.client, request, StudentExam[].class);
		assert studentExams != null;
		return Arrays.asList(studentExams);
	}
}
