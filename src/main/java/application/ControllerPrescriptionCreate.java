package application;

import java.sql.*;
import java.time.LocalDate;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;

@Controller
public class ControllerPrescriptionCreate {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	/*
	 * Doctor requests blank form for new prescription.
	 */
	@GetMapping("/prescription/new")
	public String getPrescriptionForm(Model model) {
		model.addAttribute("prescription", new PrescriptionView());
		return "prescription_create";
	}

	// process data entered on prescription_create form
	@PostMapping("/prescription")
	public String createPrescription(PrescriptionView p, Model model) {

		System.out.println("createPrescription " + p);

		/*
		 * valid doctor name and id
		 */
		//TODO
		int doctor_id = 0;
		try (Connection con = getConnection();) {
			PreparedStatement ps = con.prepareStatement(
					"select id " +
							"from doctor " +
							"where id=? and last_name=? and first_name=?");
			ps.setInt(1, p.getDoctor_id());
			ps.setString(2, p.getDoctorLastName());
			ps.setString(3, p.getDoctorFirstName());

			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				doctor_id = rs.getInt(1);
			} else {
				model.addAttribute("message", "Doctor not found.");
				model.addAttribute("prescription", p);
				return "prescription_create";
			}
		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_create";
		}
		/*
		 * valid patient name and id
		 */
		//TODO
		int patient_id = 0;
		try (Connection con = getConnection();) {
			PreparedStatement ps = con.prepareStatement(
					"select id " +
							"from patient " +
							"where id=? and last_name=? and first_name=?");
			ps.setInt(1, p.getPatient_id());
			ps.setString(2, p.getPatientLastName());
			ps.setString(3, p.getPatientFirstName());

			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				patient_id = rs.getInt(1);
			} else {
				model.addAttribute("message", "Patient not found.");
				model.addAttribute("prescription", p);
				return "prescription_create";
			}
		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_create";
		}
		/*
		 * valid drug name
		 */
		//TODO
		int drug_id = 0;
		try (Connection con = getConnection();) {
			PreparedStatement ps = con.prepareStatement(
					"select id " +
							"from drug " +
							"where name=?");
			ps.setString(1, p.getDrugName());

			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				drug_id = rs.getInt(1);
			} else {
				model.addAttribute("message", "Drug not found.");
				model.addAttribute("prescription", p);
				return "prescription_create";
			}
		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_create";
		}
		/*
		 * insert prescription  
		 */
		//TODO 
		try (Connection con = getConnection();) {
			PreparedStatement ps = con.prepareStatement(
					"insert into prescription(drug_id, doctor_id, patient_id, quantity, refills, create_date ) " +
							"values(?, ?, ?, ?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, drug_id);
			ps.setInt(2, doctor_id);
			ps.setInt(3, patient_id);
			ps.setInt(4, p.getQuantity());
			ps.setInt(5, p.getRefills());
			ps.setString(6, LocalDate.now().toString());

			ps.executeUpdate();
			ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) p.setRxid(rs.getInt(1));

			model.addAttribute("message", "Prescription created.");
			model.addAttribute("prescription", p);
			return "prescription_show";

		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_create";
		}
	}
	
	private Connection getConnection() throws SQLException {
		Connection conn = jdbcTemplate.getDataSource().getConnection();
		return conn;
	}

}
