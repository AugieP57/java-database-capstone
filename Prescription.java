@Entity
public class Prescription{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private String id;
    
    @Size(min = 3, max = 20)
    @NotNull
    private String patientName;
    @Size(min = 3, max = 100)
    @NotNull
    private Long appointmentId;
    private String medication;
    private String dosage;
    
    @Size(max = 200)
    private String doctorsNotes;

    Prescription(String id, String patientName, Long appointmentId, String medication, String dosage, String doctorsNotes){

        this.id            = id;
        this.patientName   = patientName;
        this.appointmentId = appointmentId;
        this.medication    = medication;
        this.dosage        = dosage;
        this.doctorsNotes  = doctorsNotes;
    }

    public String getPatientName() { return this.patientName; }
    public Long getAppointmentId() { return this.appointmentId; }
    public String getMedication() { return this.medication; }
    public String getDosage() { return this.dosage; }
    public String getDoctorsNotes() { return this.DoctorNotes; }

    public setPatientName() { this.patientName = patientName; }
    public setAppointmentId() { this.appointmentId = appointmentId; }
    public setMedication() { this.medication = medication; }
    public setDosage() { this.dosage = dosage; }
    public setDoctorsNotes() { this.DoctorNotes = doctorsNotes; }



}