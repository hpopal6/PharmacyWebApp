package application;

import java.sql.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;

/*
 * Controller class for patient interactions.
 *   register as a new patient.
 *   update patient profile.
 */
@Controller
public class ControllerPatientCreate {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	
	/*
	 * Request blank patient registration form.
	 */
	@GetMapping("/patient/new")
	public String getNewPatientForm(Model model) {
		// return blank form for new patient registration
		model.addAttribute("patient", new PatientView());
		return "patient_register";
	}
	
	/*
	 * Process data from the patient_register form
	 */
	@PostMapping("/patient/new")
	public String createPatient(PatientView p, Model model) {
		/*
		 * validate doctor last name and find the doctor id
		 */
		// TO-DO
		int doctor_id = 0;
		try (Connection con = getConnection();) {
			PreparedStatement ps = con.prepareStatement("select id from doctor where last_name=?");
			ps.setString(1, p.getPrimaryName());

			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				doctor_id = rs.getInt(1);
			} else {
				model.addAttribute("message", "Doctor not found.");
				model.addAttribute("patient", p);
				return "patient_register";
			}
		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("patient", p);
			return "patient_register";
		}
		/*
		 * insert to patient table
		 */
		try (Connection con = getConnection();) {
			PreparedStatement ps = con.prepareStatement(
					"insert into patient(doctor_id, ssn, birthdate, last_name, first_name, " +
							"street, city,  state, zipcode) " +
							"values(?, ?, ?, ?, ?, ?, ?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, doctor_id);
			ps.setString(2, p.getSsn());
			ps.setString(3,p.getBirthdate());
			ps.setString(4, p.getLast_name());
			ps.setString(5,p.getFirst_name());
			ps.setString(6,p.getStreet());
			ps.setString(7, p.getCity());
			ps.setString(8, p.getState());
			ps.setString(9,p.getZipcode());

			ps.executeUpdate();

			ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) p.setId(rs.getInt(1));

			// display patient data and the generated patient ID,  and success message
			// display message and patient information
			model.addAttribute("message", "Registration successful.");
			model.addAttribute("patient", p);
			return "patient_show";

			/*
			 * on error
			 * model.addAttribute("message", some error message);
			 * model.addAttribute("patient", p);
			 * return "patient_register";
			 */
		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("patient", p);
			return "patient_register";
		}


	}

	/*
	 * Request blank form to search for patient by id and name
	 */
	@GetMapping("/patient/edit")
	public String getSearchForm(Model model) {
		model.addAttribute("patient", new PatientView());
		return "patient_get";
	}

	/*
	 * Perform search for patient by patient id and name.
	 */
	@PostMapping("/patient/show")
	public String showPatient(PatientView p, Model model) {

		// TODO   search for patient by id and name

		// if found, return "patient_show", else return error message and "patient_get"

		return "patient_show";
	}

	/*
	 * return JDBC Connection using jdbcTemplate in Spring Server
	 */

	private Connection getConnection() throws SQLException {
		Connection conn = jdbcTemplate.getDataSource().getConnection();
		return conn;
	}
}
