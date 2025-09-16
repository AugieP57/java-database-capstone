@Entity 
class Admin {
  
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY))
    Private int id
    
    @NotNull
    Private String username
    
    @NotNull
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password
    
    function Admin(String username, String password){
        
      this.username = username;
      this.password = password;
    }
  
    public String getUsername(){ return this.username; }
    
    public setUsername(String username){ this.username = username; }
    public setPassword(String password){ this.password = password; }
  
  }
  

