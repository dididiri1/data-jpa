

### 라이브러리 살펴보기

- **gradle** 의존관계 보기

``` cmd
 ./gradlew dependencies --configuration compileClasspath
``` 

### 쿼리 파라미터 로그 남기기

- 로그에 다음을 추가하기 org.hibernate.type : SQL 실행 파라미터를 로그로 남긴다.
- 외부 라이브러리 사용
``` yml
  implementation 'com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.5.6'
``` 

> 참고: 쿼리 파라미터를 로그로 남기는 외부 라이브러리는 시스템 자원을 사용하므로, 개발 단계에서는 편하게 사용해도 된다.
> 하지만 운영시스템에 적용하려면 꼭 성능테스트를 하고 사용하는 것이 좋다.

## 예제 도메인 모델

### 예제 도메인 모델과 동작확인

![](https://github.com/dididiri1/data-jpa/blob/main/study/images/01_01.png?raw=true)


- ERD
![](https://github.com/dididiri1/data-jpa/blob/main/study/images/01_02.png?raw=true)


- 꿀팁 (JAP 엔티티 빨간줄 해줌)

![](https://github.com/dididiri1/data-jpa/blob/main/study/images/01_03.png?raw=true)


- Member 엔티티

``` java
@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "username", "age"})
public class Member {

    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long id;
    
    private String username;
    
    private int age;
     
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;
    
    public Member(String username) {
        this(username, 0);
    }
    
    public Member(String username, int age) {
        this(username, age, null);
    }
    
    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        if (team != null) {
            changeTeam(team);
        }
     }
     
     public void changeTeam(Team team) {
         this.team = team;
         team.getMembers().add(this);
     }
}
```
- 롬복 설명
  - @Setter: 실무에서 가급적 Setter는 사용하지 않기
  - @NoArgsConstructor AccessLevel.PROTECTED: 기본 생성자 막고 싶은데, JPA 스팩상 PROTECTED로 열어두어야 함
  - @ToString은 가급적 내부 필드만(연관관계 없는 필드만)
- changeTeam() 으로 양방향 연관관계 한번에 처리(연관관계 편의 메소드)

``` java
@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "name"})
public class Team {

     @Id @GeneratedValue
     @Column(name = "team_id")
     private Long id;
     
     private String name;
     
     @OneToMany(mappedBy = "team")
     List<Member> members = new ArrayList<>();
     public Team(String name) {
         this.name = name;
     }
}
```

- Member와 Team은 양방향 연관관계, Member.team 이 연관관계의 주인, Team.members 는 연관관계의  
  주인이 아님, 따라서 Member.team 이 데이터베이스 외래키 값을 변경, 반대편은 읽기만 가능


- 가급적 순수 JPA로 동작 확인 (뒤에서 변경)
- db 테이블 결과 확인
- 지연 로딩 동작 확인

### 공통 인터페이스 기능
- 순수 JPA 기반 리포지토리 만들기
- 스프링 데이터 JPA 공통 인터페이스 소개
- 스프링 데이터 JPA 공통 인터페이스 활용

- 꿀팁 변수명 한번에 바꾸기 (쉬프트 + f6)


### 공통 인터페이스 설정