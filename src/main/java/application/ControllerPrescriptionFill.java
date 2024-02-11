package application;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;

@Controller
public class ControllerPrescriptionFill {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/*
	 * Patient requests form to fill prescription.
	 */
	@GetMapping("/prescription/fill")
	public String getfillForm(Model model) {
		model.addAttribute("prescription", new PrescriptionView());
		return "prescription_fill";
	}

	// process data from prescription_fill form
	@PostMapping("/prescription/fill")
	public String processFillForm(PrescriptionView p, Model model) {

		/*
		 * valid pharmacy name and address, get pharmacy id and phone
		 */
		// TODO

		int pharmacy_id = 0;
		String phone = "";

		try (Connection con = getConnection();) {
			PreparedStatement ps = con.prepareStatement(
					"select id, phone " +
							"from pharmacy " +
							"where name=? and address=?");
			ps.setString(1, p.getPharmacyName());
			ps.setString(2, p.getPharmacyAddress());

			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				pharmacy_id = rs.getInt(1);
				phone = rs.getString(2);
			} else {
				model.addAttribute("message", "Pharmacy not found.");
				model.addAttribute("prescription", p);
				return "prescription_fill";
			}
		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_fill";
		}

		// TODO find the patient information
		int patient_id = 0;
		String p_last_name = "";
		String p_first_name = "";
		try (Connection con = getConnection();) {
			PreparedStatement ps = con.prepareStatement(
					"select id, last_name, first_name " +
							"from patient " +
							"where last_name=?");
			ps.setString(1, p.getPatientLastName());

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				patient_id = rs.getInt(1);
				p_last_name = rs.getString(2);
				p_first_name = rs.getString(3);
			} else {
				model.addAttribute("message", "Patient not found.");
				model.addAttribute("prescription", p);
				return "prescription_fill";
			}
		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_fill";
		}

		// TODO find the prescription 
		int rxid_id = 0;
		int doctor_id = 0;
		int drug_id = 0;
		int refills = 0;
		try (Connection con = getConnection();) {
			PreparedStatement ps = con.prepareStatement(
					"select rxid, doctor_id, drug_id, refills" +
							"from prescription " +
							"where rxid=?");
			ps.setInt(1, p.getRxid());

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				rxid_id = rs.getInt(1);
				doctor_id = rs.getInt(2);
				drug_id = rs.getInt(3);
				refills = rs.getInt(4);
			} else {
				model.addAttribute("message", "Prescription not found.");
				model.addAttribute("prescription", p);
				return "prescription_fill";
			}
		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_fill";
		}

		/*
		 * have we exceeded the number of allowed refills
		 * the first fill is not considered a refill.
		 */
		
		// TODO
		int count = 0;
		try (Connection con = getConnection();) {
			PreparedStatement ps = con.prepareStatement(
					"select count(rxid)" +
							"from prescription_fill " +
							"where rxid=?");
			ps.setInt(1, p.getRxid());

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				count = rs.getInt(1);
			}
			int remaining = refills - count;
			if (remaining < 1 ){
				model.addAttribute("message", "No prescription refills remaining.");
				model.addAttribute("prescription", p);
				return "prescription_fill";
			} else {
				model.addAttribute("message", "Prescription refills remaining: " + remaining);
				model.addAttribute("prescription", p);
			}
		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_fill";
		}

		
		/*
		 * get doctor information 
		 */
		// TODO
		String dr_last_name = "";
		String dr_first_name = "";
		try (Connection con = getConnection();) {
			PreparedStatement ps = con.prepareStatement(
					"select last_name, first_name " +
							"from doctor " +
							"where id=?");
			ps.setInt(1, doctor_id);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				dr_last_name = rs.getString(2);
				dr_first_name = rs.getString(3);
			} else {
				model.addAttribute("message", "Doctor not found.");
				model.addAttribute("prescription", p);
				return "prescription_fill";
			}
		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_fill";
		}


		/*
		 * calculate cost of prescription
		 */
		// TODO 
		int qtyPharmacy = p.getQuantity();
		BigDecimal cost = new BigDecimal("0.0");
		BigDecimal price = new BigDecimal("0.0");
		try (Connection con = getConnection();) {
			PreparedStatement ps = con.prepareStatement(
					"select quantity, price " +
							"from drug_cost " +
							"where drug_id=? and pharmacy_id=?");
			ps.setInt(1, drug_id);
			ps.setInt(1, pharmacy_id);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				qtyPharmacy = rs.getInt(1);
				price = rs.getBigDecimal(2);
				cost = price.multiply(BigDecimal.valueOf(p.getQuantity()));
			} else {
				model.addAttribute("message", "Drug not found.");
				model.addAttribute("prescription", p);
				return "prescription_fill";
			}
		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_fill";
		}

		// TODO save updated prescription
		// insert record to prescription_fill
		try (Connection con = getConnection();) {
			PreparedStatement ps = con.prepareStatement(
					"insert into prescription_fill(rxid_id, pharmacy_id, cost, fill_date) " +
							"values(?, ?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, rxid_id);
			ps.setInt(2, pharmacy_id);
			ps.setBigDecimal(3, cost);
			ps.setString(4, LocalDate.now().toString());

			ps.executeUpdate();
			ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) p.setRxid(rs.getInt(1));
		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_fill";
		}

		// update prescription



		// show the updated prescription with the most recent fill information
		model.addAttribute("message", "Prescription filled.");
		model.addAttribute("prescription", p);
		return "prescription_show";
	}
	
	private Connection getConnection() throws SQLException {
		Connection conn = jdbcTemplate.getDataSource().getConnection();
		return conn;
	}

}