@Entity
class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  
    private Long id;
    
    private String name;
  
    @Email
     private String email;
  
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    
    @Pattern(regexp = "\\d{10}", message = "Phone number must be 10 digits")
    private String phone;
  
    @Size(max = 255)
    private String address;

    Patient( String name, String email, String password, String phone, String address){
        
        this.name     = name;
        this.email    = email;
        this.password = password;
        this.phone    = phone;
        this.address =  address;
  }

     public Long getID(){ return this id; } 
     public String getName(){ return this.name; }
     public String getEmail(){ return this.email; }
     public String getPhone(){ return this.phone; }
     public String getAddress(){ return this.address; } 

    public setName(String name){ this.name = name}
    public setEmail(String email){ this.address = email; }
    public setPhone(String phone){ this.phone = phone; }
    public setAddress(String address){ this.address = address; }

}
