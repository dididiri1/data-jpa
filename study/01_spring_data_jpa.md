

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


***꿀팁 변수명 한번에 바꾸기 (쉬프트 + f6)***


***H2 데이터 베이스 비밀번호 초기화***

```sql
 ALTER USER SA SET PASSWORD '';
``` 

### 공통 인터페이스 설정

### JavaConfig 설정- 스프링 부트 사용시 생략 가능
```java
  @Configuration
  @EnableJpaRepositories(basePackages = "jpabook.jpashop.repository")
  public class AppConfig {}
```

- 스프링부트사용시@SpringBootApplication 위치를지정(해당패키지와하위패키지인식)'
- 만약 위치가 달라지면 @EnableJpaRepositories 필요

### 스프링 데이터 JPA가 구현 클래스 대신 생성

![](https://github.com/dididiri1/data-jpa/blob/main/study/images/01_04.png?raw=true)

- org.springframework.data.repository.Repository 를 구현한 클래스는 스캔 대상
  - MemberRepository 인터페이스가 동작한 이유
  - 실제 출력해보기(Proxy)
  - memberRepository.getClass() class com.sun.proxy.$ProxyXXX
- @Repository 애노테이션 생략 가능
```java 
  // @Repository
  public interface TeamRepository extends JpaRepository<Team, Long> {

  }
``` 
  - 컴포넌트 스캔을 스프링 데이터 JPA가 자동으로 처리
  - JPA 예외를 스프링 예외로 변환하는 과정도 자동으로 처리


### 제네릭 타입

- T : 엔티티
- ID : 엔티티의 식별자 타입 
- S : 엔티티와 그 자식 타입

### 주요 메서드

- save(S) : 새로운 엔티티는 저장하고 이미 있는 엔티티는 병합한다.
- delete(T) : 엔티티 하나를 삭제한다. 내부에서 EntityManager.remove() 호출
- findById(ID) : 엔티티 하나를 조회한다. 내부에서 EntityManager.find() 호출
- getOne(ID) : 엔티티를 프록시로 조회한다. 내부에서 EntityManager.getReference() 호출
- findAll(...) : 모든 엔티티를 조회한다. 정렬( Sort )이나 페이징( Pageable ) 조건을 파라미터로 제공할
  수 있다.

> 참고: JpaRepository 는 대부분의 공통 메서드를 제공한다.

## 쿼리 메소드 기능

- 메소드 이름으로 쿼리 생성
- NamedQuery
- @Query - 리파지토리 메소드에 쿼리 정의 파라미터 바인딩
- 반환 타입
- 페이징과 정렬
- 벌크성 수정 쿼리
- @EntityGraph

***스프링 데이터 JPA가 제공하는 마법 같은 기능***

### 쿼리 메소드 기능 3가지

- 1. 메소드 이름으로 쿼리 생성
- 2. 메소드 이름으로 JPA NamedQuery 호출
- 3. @Query 어노테이션을 사용해서 리파지토리 인터페이스에 쿼리 직접 정의

- 메소드 이름으로 쿼리 생성

메소드 이름을 분석해서 JPQL 쿼리 실행 

이름과 나이를 기준으로 회원을 조회하려면?

- 순수 JPA 리포지토리
``` java
  public List<Member> findByUsernameAndAgeGreaterThan(String username, int age) {
      return em.createQuery("select m from Member m where m.username = :username and m.age > :age")
              .setParameter("username", username)
              .setParameter("age", age)
              .getResultList();
  }

``` 

- 스프링 데이터 JPA
``` java
 public interface MemberRepository extends JpaRepository<Member, Long> {
    
    List<Member> findByUsernameAndAgeGreaterThan(String username, int age);
 
 }
``` 
**쿼리 메소드 필터 조건**

스프링 데이터 JPA 공식 문서 참고: (https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.query-creation)

**스프링 데이터 JPA가 제공하는 쿼리 메소드 기능**
- 조회: find...By ,read...By ,query...By get...By,
  - https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.query-creation
  - 예:) findHelloBy 처럼 ..에 식별하기 위한 애용이 들어가도 된다.
  
- COUNT: count...By 반환타입 long
- EXISTS: exists...By 반환타입 boolean
- 삭제: delete...By, remove...By 반환타입 long
- DISTINCT: findDistinct, findMemberDistinctBy
- LIMIT: findFirst3, findFirst, findTop, findTop3
  - https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.limit-query-result

> 참고: 이 기능은 엔티티의 필드명이 변경되면 인터페이스에 정의한 메서드 이름도 꼭 함께 변경해야 한다. 
> 그렇지 않으면 애플리케이션을 시작하는 시점에 오류가 발생한다.
> 이렇게 애플리케이션 로딩 시점에 오류를 인지할 수 있는 것이 스프링 데이터 JPA의 매우 큰 장점이다.
