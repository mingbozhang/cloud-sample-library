package com.sap.hana.cloud.samples.book.service;

import java.io.IOException;
import java.util.Date;

import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sap.hana.cloud.samples.util.CalendarUtils;

import com.sap.hana.cloud.samples.adapters.IdentityAdapter;
import com.sap.hana.cloud.samples.adapters.PersistenceAdapter;
import com.sap.hana.cloud.samples.persistence.entities.Book;
import com.sap.hana.cloud.samples.persistence.entities.BookLending;
import com.sap.hana.cloud.samples.persistence.entities.LibraryUser;

/**
 * This class reserves a book to the logged-on user.
 * 
 * The status of the {@link com.sap.hana.cloud.samples.persistence.entities.Book Book} is updated, as well as
 * the list of the {@link com.sap.hana.cloud.samples.persistence.entities.Book Books} which the user has taken
 * from the library.
 * 
 * This servlet can be called by all users. 
 * */
@WebServlet(name="ReserveBookServlet",
urlPatterns={"/restricted/everyone/ReserveBookServlet"})
public class ReserveBookServlet extends HttpServlet {

	private static final long serialVersionUID = 1;

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		Date now = new Date();
		Date deadLine = CalendarUtils.getDateByAddMonths(now, 1);

		String currentUserId = IdentityAdapter.getLoggedUserId(request);

		EntityManager em = PersistenceAdapter.getEntityManager();
		LibraryUser userFromDatabase = (LibraryUser) em.createNamedQuery("getUserById").setParameter("userId", currentUserId).getSingleResult();

		String title = request.getParameter("bookName").trim();
	    String author = request.getParameter("authorName").trim();
		Book bookToReserve = (Book) em.createNamedQuery("bookByTitleAndAuthor").setParameter("bookName", title).setParameter("authorName", author).getSingleResult();
								
		bookToReserve = updateBookStatus(bookToReserve, userFromDatabase, deadLine);
		
		BookLending lending = createLendedBookEntry(bookToReserve, userFromDatabase, now, deadLine);	
		
		em.getTransaction().begin();
		em.merge(bookToReserve);
		em.persist(lending);
		em.getTransaction().commit();
		
	}

	private BookLending createLendedBookEntry(Book bookToReserve, LibraryUser userFromDatabase, Date now, Date deadLine) {
		
		int remainingDays = CalendarUtils.getDaysBetween(now, deadLine);
        BookLending lending = new BookLending();
        lending.setLendedBook(bookToReserve);
        lending.setUser(userFromDatabase);
        lending.setRemainingDays(remainingDays);

        return lending;
	}

	private Book updateBookStatus(Book bookToReserve, LibraryUser userFromDatabase, Date deadLine) {    
		
        bookToReserve.setReservedUntil(deadLine);
        bookToReserve.setReservedBy(userFromDatabase.getDisplayName());
        bookToReserve.setReservedByUserId(userFromDatabase.getUserId());
        bookToReserve.setReserved(true);
		
		return bookToReserve;
	}
}
