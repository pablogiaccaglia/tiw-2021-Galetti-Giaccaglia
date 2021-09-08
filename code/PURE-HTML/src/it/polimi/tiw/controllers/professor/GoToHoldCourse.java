package it.polimi.tiw.controllers.professor;

import java.io.IOException;
import java.io.Serial;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.thymeleaf.TemplateEngine;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import it.polimi.tiw.beans.Course;
import it.polimi.tiw.beans.Exam;
import it.polimi.tiw.beans.Professor;
import it.polimi.tiw.dao.CourseDAO;
import it.polimi.tiw.dao.ExamDAO;
import it.polimi.tiw.utils.ConnectionHandler;
import it.polimi.tiw.utils.ForwardHandler;
import it.polimi.tiw.utils.PathUtils;
import it.polimi.tiw.utils.TemplateHandler;

/**
 * Servlet implementation class ToHoldCoursePage
 */
@WebServlet("/GoToHoldCourse")
public class GoToHoldCourse extends HttpServlet {

	@Serial
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	private Connection connection;

	@Override
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		this.templateEngine = TemplateHandler.getEngine(servletContext, ".html");
		this.connection = ConnectionHandler.getConnection(servletContext);
	}

	@Override
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String courseIdString = request.getParameter("courseId");
		int courseId;

		if(courseIdString == null) {
			ForwardHandler.forwardToErrorPage(request, response, "Null courseId, when accessing course details", templateEngine);
			return;
		}

		try {
			courseId = Integer.parseInt(courseIdString);
		}catch (NumberFormatException e) {
			ForwardHandler.forwardToErrorPage(request, response, "Chosen course id is not a number, when accessing courses details", templateEngine);
			return;
		}


		CourseDAO courseDAO = new CourseDAO(connection);
		ExamDAO examDAO = new ExamDAO(connection);
		Course course;
		List<Exam> exams;
		HttpSession session = request.getSession(false);
		Professor currentProfessor = (Professor)session.getAttribute("professor");
		
		if(currentProfessor==null) {
			ForwardHandler.forwardToErrorPage(request, response, "You are not authorized to perform this action!", templateEngine);
			return;		
		}

		//fetching professor courses to get updated courses list
		try {
			currentProfessor.setCourses(courseDAO.getCoursesByProfessorId(currentProfessor.getId()));
		} catch (SQLException e) {
			ForwardHandler.forwardToErrorPage(request, response, "There has been an error finding courses by professor ID", templateEngine);
			return;		
		}
		
		try {
			if(courseDAO.isCourseIdNotValid(courseId)) {
				ForwardHandler.forwardToErrorPage(request, response,  "Course id doesn't match any currently active course" , templateEngine);
				return;
			}
		} catch (SQLException e) {
			ForwardHandler.forwardToErrorPage(request, response, e.getMessage(), templateEngine);
			return;		
		}

		if(currentProfessor.getCourseById(courseId).isEmpty()) {
			ForwardHandler.forwardToErrorPage(request, response, "Course is not held by you", templateEngine);
			return;
		}

		course =  currentProfessor.getCourseById(courseId).get();
		
		try {
			exams = examDAO.getExamsByCourseId(courseId);
			
		} catch (SQLException e) {
			ForwardHandler.forwardToErrorPage(request, response, e.getMessage(), templateEngine);
			return;		
		}

		request.setAttribute("course", course);
		request.setAttribute("exams", exams);
		ForwardHandler.forward(request, response, PathUtils.pathToCoursePage, templateEngine);

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}