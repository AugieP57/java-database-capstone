@Entity
class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
    
	private String name;
    private String speciality;
    
    @Email
    private String email;
    
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    
    @Pattern(regexp = "\\d{10}", message = "Phone number must be 10 digits")
    private String phone;
    
    @ElementCollection
    private List<String> availableTimes;

    Doctor(Long id, String name, String speciality, String email, String password, String phone, List<String> availableTimes;){

        this.id             = id;
        this.name           = name;
        this.speciality     = speciality;
        this.email          = email;
        this password       = password;
        this.phone          = phone;
        this.availableTimes = availableTimes;
  }
  
    public Long getID(){ return this.id; }
    public String getName(){ return this.name; }
    public String getSpeciality(){ return this.speciality; }
    public String getEmail(){ return this.email; }
    public String getPhone(){ return this.phone; }
    public List<String> getAvailableTimes(){ return this.availableTimes; }
    
    public setName(String name){ this.name = name; }
    public setSpeciality(String speciality){ this.speciality = speciality; }
    public setEmail(String email){ this.email = email; }
    public setPhone(String name){ this.phone = phone; }
    public setAvailableTimes(String availableTimes){ this.availableTimes = availableTimes; }
  
}