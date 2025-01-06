package application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * Controller class for patient interactions.
 *   register as a new patient.
 *   update patient profile.
 */
@Controller
public class ControllerPatientUpdate {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	/*
	 *  Display patient profile for patient id.
	 */
	@GetMapping("/patient/edit/{id}")
	public String getUpdateForm(@PathVariable int id, Model model) {

		System.out.println("getUpdateForm "+ id);

		PatientView pv = new PatientView();

		pv.setId(id);

		try (Connection con = getConnection();) {
			PreparedStatement ps = con.prepareStatement(
					"select p.last_name, p.first_name, " +
							"p.street, p.city, p.state, p.zipcode, p.birthdate," +
							"d.last_name " +
							"from patient p " +
							"join doctor d on p.doctor_id = d.id " +
							"where p.id=?");
			ps.setInt(1,  id);

			ResultSet rs = ps.executeQuery();
			if(rs.next()){
				pv.setLast_name(rs.getString(1));
				pv.setFirst_name(rs.getString(2));
				pv.setStreet(rs.getString(3));
				pv.setCity(rs.getString(4));
				pv.setState(rs.getString(5));
				pv.setZipcode(rs.getString(6));
				pv.setBirthdate(rs.getString(7));
				pv.setPrimaryName(rs.getString(8));
				model.addAttribute("patient", pv);
				return "patient_edit";
			} else {
				model.addAttribute("message", "Patient not found.");
				model.addAttribute("patient", pv);
				return "patient_get";
			}
		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("patient", pv);
			return "patient_get";
		}
		// TODO search for patient by id
		//  if not found, return to home page using return "index"; 
		//  else create PatientView and add to model.
		// model.addAttribute("message", some message);
		// model.addAttribute("patient", pv
		// return editable form with patient data
		//return "patient_edit";
}
	/*
	 * Process changes from patient_edit form
	 *  Primary doctor, street, city, state, zip can be changed
	 *  ssn, patient id, name, birthdate, ssn are read only in template.
	 */
	@PostMapping("/patient/edit")
	public String updatePatient(PatientView p, Model model) {
		System.out.println("updatePatient " + p);
		// validate doctor last name
		int doctor_id = 0;

		try (Connection con = getConnection();) {
			PreparedStatement ps = con.prepareStatement(
					"select id " +
							"from doctor " +
							"where last_name=?");
			ps.setString(1, p.getPrimaryName());

			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				doctor_id = rs.getInt(1);
			} else {
				model.addAttribute("message", "Doctor not found.");
				model.addAttribute("patient", p);
				return "patient_edit";
			}
		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("patient", p);
			return "patient_edit";
		}
		
		// TODO 
		
		// TODO update patient profile data in database
		//Primary doctor, street, city, state, zip can be changed
		try (Connection con = getConnection();) {
			PreparedStatement ps = con.prepareStatement(
					"update patient " +
							"set street=?, city=?, state=?, zipcode=?, doctor_id=?" +
							" where id=?");
			ps.setString(1, p.getStreet());
			ps.setString(2, p.getCity());
			ps.setString(3, p.getState());
			ps.setString(4, p.getZipcode());
			ps.setInt(5,doctor_id);
			ps.setInt(6, p.getId());

			// rc is row count from executeUpdate
			// should be 1
			int rc = ps.executeUpdate();

			if (rc == 1) {
				model.addAttribute("message", "Update successful");
				model.addAttribute("patient", p);
				return "patient_show";

			} else {
				model.addAttribute("message", "Error. Update was not successful");
				model.addAttribute("patient", p);
				return "patient_edit";
			}

		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error." + e.getMessage());
			model.addAttribute("patient", p);
			return "patient_edit";
		}

		// model.addAttribute("message", some message);
		// model.addAttribute("patient", p)
		//return "patient_show";
	}

	private Connection getConnection() throws SQLException {
		Connection conn = jdbcTemplate.getDataSource().getConnection();
		return conn;
	}
	
}
