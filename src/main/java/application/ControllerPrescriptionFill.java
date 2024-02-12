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
		String pharmacyPhone = "";
		String pharmacyName = "";
		String pharmacyAddress = "";

		try (Connection con = getConnection();) {
			PreparedStatement ps = con.prepareStatement(
					"select id, phone, name, address " +
							"from pharmacy " +
							"where name=? and address=?");
			ps.setString(1, p.getPharmacyName());
			ps.setString(2, p.getPharmacyAddress());

			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				pharmacy_id = rs.getInt(1);
				pharmacyPhone = rs.getString(2);
				System.out.println("pharmacy");
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
				System.out.println("patient");
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
		int rxQuantity = 0;
		String drugName = "";
		try (Connection con = getConnection();) {
			PreparedStatement ps = con.prepareStatement(
					"select p.rxid, p.doctor_id, p.drug_id, p.refills, p.quantity, d.name " +
							"from prescription p " +
							"join drug d on p.drug_id = d.id " +
							"where p.rxid=?");
			ps.setInt(1, p.getRxid());

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				rxid_id = rs.getInt(1);
				doctor_id = rs.getInt(2);
				drug_id = rs.getInt(3);
				refills = rs.getInt(4);
				rxQuantity = rs.getInt(5);
				drugName = rs.getString(6);
				System.out.println("prescription drug");
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
					"select count(rxid_id) " +
							"from prescription_fill " +
							"where rxid_id=?");
			ps.setInt(1, p.getRxid());

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				count = rs.getInt(1);
				System.out.println("prescription_fill, refills/count: " + refills +"/" + count);
			}
			refills = refills + 1 - count; // +1 is because every prescription can be filled at least once
			if (refills < 1 ){
				model.addAttribute("message", "No prescription refills remaining.");
				model.addAttribute("prescription", p);
				return "prescription_fill";
			} else {
				model.addAttribute("message", "Prescription refills remaining: " + refills);
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
				dr_last_name = rs.getString(1);
				dr_first_name = rs.getString(2);
				System.out.println("doctor");
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
		refills--;
		int qtyPharmacy = 0;
		BigDecimal cost = new BigDecimal("0.0");
		BigDecimal price = new BigDecimal("0.0");
		String fillDate = LocalDate.now().toString();
		try (Connection con = getConnection();) {
			PreparedStatement ps = con.prepareStatement(
					"select quantity, price " +
							"from drug_cost " +
							"where drug_id=? and pharmacy_id=?");
			ps.setInt(1, drug_id);
			ps.setInt(2, pharmacy_id);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				qtyPharmacy = rs.getInt(1);
				price = rs.getBigDecimal(2);
				cost = price.multiply(BigDecimal.valueOf(rxQuantity));

				p.setRxid(rxid_id);

				p.setDoctor_id(doctor_id);
				p.setDoctorFirstName(dr_first_name);
				p.setDoctorLastName(dr_last_name);

				p.setPatient_id(patient_id);
				p.setPatientFirstName(p_first_name);
				p.setPatientLastName(p_last_name);

				p.setDrugName(drugName);
				p.setQuantity(rxQuantity);
				p.setRefillsRemaining(refills);
				p.setRefills(refills);

				p.setPharmacyID(pharmacy_id);
				p.setPharmacyName(p.getPharmacyName());
				p.setPharmacyAddress(p.getPharmacyAddress());
				p.setPharmacyPhone(pharmacyPhone);

				p.setDateFilled(fillDate);
				p.setCost(cost.toString());
				System.out.println("rxid_id: " + rxid_id);
				System.out.println("cost: " + cost + " : " + cost.toString());

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
			ps.setString(4, fillDate);

			ps.executeUpdate();
			ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) p.setRxid(rs.getInt(1));
		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_fill";
		}

		// update prescription
//		try (Connection con = getConnection();) {
//			PreparedStatement ps = con.prepareStatement(
//					"update prescription set refills=? where rxid=?");
//			ps.setInt(1, refills);
//			ps.setInt(2, rxid_id);
//
//			// rc is row count from executeUpdate
//			// should be 1
//			int rc = ps.executeUpdate();
//
//			if (rc == 1) {
//				// show the updated prescription with the most recent fill information
//				model.addAttribute("message", "Prescription filled.");
//				model.addAttribute("prescription", p);
//				return "prescription_show";
//			} else {
//				model.addAttribute("message", "Error. Update was not successful");
//				model.addAttribute("prescription", p);
//				return "prescription_fill";
//			}
//		} catch (SQLException e) {
//			model.addAttribute("message", "SQL Error."+e.getMessage());
//			model.addAttribute("prescription", p);
//			return "prescription_fill";
//		}

//
//		// show the updated prescription with the most recent fill information
		model.addAttribute("message", "Prescription filled.");
		model.addAttribute("prescription", p);
		return "prescription_show";
	}
	
	private Connection getConnection() throws SQLException {
		Connection conn = jdbcTemplate.getDataSource().getConnection();
		return conn;
	}

}