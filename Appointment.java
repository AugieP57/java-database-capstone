import java.time.LocalDateTime;

@Entity
public class Appointment {
  
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @NotNull
    private String Doctor;
    
    @ManyToOne
    @NotNull
    private String Patient;
    
    @Future
    private LocalDateTime appointmentTime;
    private int status;

    Appointment(Long id, String doctor, String patient, LocalDateTime appointmentTime, int status){

        this.id              = id;
        this.Doctor          = doctor;
        this.Patient         = patient;
        this.appointmentTime = appointmentTime;
        this.status           = status;
    }
  
    public Long getID(){ return this.id; }
    public String getDoctor(){ return this.doctor; }
    public String getPatient(){ return this.patient; }
    public String getAppointmentTime(){ return this.appointmentTime; }
    public String getStatus(){ return this.status; }  

    public setID(Long id){ this.id = id; }
    public setDoctor(String doctor){ this.doctor = doctor; }
    public setPatient(String patient){ this.date = patient; }
    public setAppiontTime(String appointmentTime){ this.appointmentTime = appointmentTime; }
    public setStatus(){ this.status = status; }

    @Transient
    public getEndTime(LocalDateTime appointmentTime){ } // Returns the end time of the appointment (1 hour after start time)
    @Transient
    public getAppointmentDate(LocalDateTime appointmentTime){ } //  Returns only the date portion of the appointment
    @Transient
    public getAppointmentTimeOnly(LocalDateTime appointmentTime){ } // Returns only the time portion of the

}