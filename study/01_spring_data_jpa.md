
## tdd
- given 이런 상황이(데이터) 주어줬을때
- when 이렇케 실행하면
- then 이런 결과가 나온다.

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


#### Member 엔티티
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
  
#### @Repository 애노테이션 생략 가능
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
- @Query - 리파지토리 메소드에 쿼리 정의 
- 파라미터 바인딩
- 반환 타입
- 페이징과 정렬
- 벌크성 수정 쿼리
- @EntityGraph

***스프링 데이터 JPA가 제공하는 마법 같은 기능***

### 쿼리 메소드 기능 3가지

- 1. 메소드 이름으로 쿼리 생성
- 2. 메소드 이름으로 JPA NamedQuery 호출
- 3. @Query 어노테이션을 사용해서 리파지토리 인터페이스에 쿼리 직접 정의


### 메소드 이름으로 쿼리 생성

메소드 이름을 분석해서 JPQL 쿼리 실행 

이름과 나이를 기준으로 회원을 조회하려면?

#### 순수 JPA 리포지토리
``` java
  public List<Member> findByUsernameAndAgeGreaterThan(String username, int age) {
      return em.createQuery("select m from Member m where m.username = :username and m.age > :age")
              .setParameter("username", username)
              .setParameter("age", age)
              .getResultList();
  }

``` 

#### 스프링 데이터 JPA
``` java
 public interface MemberRepository extends JpaRepository<Member, Long> {
    
    List<Member> findByUsernameAndAgeGreaterThan(String username, int age);
 
 }
``` 
**1. 쿼리 메소드 필터 조건**

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
> 그렇지 않으면 애플리케이션을 시작하는 시점에 오류가 발생한다. 이렇게 애플리케이션 로딩 시점에   
> 오류를 인지할 수 있는것이 스프링 데이터 JPA의 매우 큰 장점이다.

### JPA NamedQuery

JPA의 NamedQuery를 호출할 수 있음

#### @NamedQuery 어노테이션으로 Named 쿼리 정의
``` java
  @Entity
  @NamedQuery(
      name="Member.findByUsername",
      query="select m from Member m where m.username = :username")
  public class Member {
      ... 
}
 
```

#### JPA를 직접 사용해서 Named 쿼리 호출
``` java
  public List<Member> findByUsername(String username) {
        return em.createNamedQuery("Member.findByUsername", Member.class)
                .setParameter("username", "회원1")
                .getResultList();
  }
``` 

#### 스프링 데이터 JPA로 NamedQuery 사용
``` java
@Query(name = "Member.findByUsername")
List<Member> findByUsername(@Param("username") String username);
``` 

장점: 애플리케이션 로딩 시점에 문법 오류를 체크함. JPQL은 문법 오류 안나는데 사용자가 눌렀을때 에러남.

> 참고: 스프링 데이터 JPA를 사용하면 실무에서 Named Query를 직접 등록해서 사용하는 일은 드물다.   
> 대신 @Query 를 사용해서 리파지토리 메소드에 쿼리를 직접 정의한다.

- @Query, 리포지토리 메소드에 쿼리 정의하기

***이름이 없는 NamedQuery 라고 생각하면 된다.***

``` java
@Query("select m from Member m where m.username= :username and m.age = :age")
List<Member> findUser(@Param("username") String username, @Param("age") int age);
``` 

- @org.springframework.data.jpa.repository.Query 어노테이션을 사용
- 실행할 메서드에 정적 쿼리를 직접 작성하므로 이름 없는 Named 쿼리라 할 수 있음
- JPA Named 쿼리처럼 애플리케이션 실행 시점에 문법 오류를 발견할 수 있음(매우 큰 장점!)

> 참고: 실무에서는 메소드 이름으로 쿼리 생성 기능은 파라미터가 증가하면 메서드 이름이 매우 지저분해진다.  
> 따라서 @Query 기능을 자주 사용하게 된다.


### @Query, 값, DTO 조회하기

#### 단순히 값 하나를 조회
``` java
  @Query("select m.username from Member m")
  List<String> findByUsernameList();
``` 

#### DTO로 직접 조회
``` java
  @Query("select new study.datajpa.dto.MemberDto(m.id, m.username, t.name) from Member m join m.team t")
  List<MemberDto> findMemberDto();
``` 

### 파라미터 바인딩

- 위치 기반 
- 이름 기반
``` java
select m from Member m where m.username = ?0 //위치 기반 
 
select m from Member m where m.username = :name //이름 기반
``` 

> 참고: 코드 가독성과 유지보수를 위해 이름 기반 파라미터 바인딩을 사용하자

#### 컬렉션 파라미터 바인딩 (Collection 타입으로 in절 지원)
``` java
@Query("select m from Member m where m.username in :names")
List<Member> findByNames(@Param("names") Collection<String> names);
``` 

### 반환 타입

#### 스프링 데이터 JPA는 유연한 반환 타입 지원
``` java
 List<Member> findListByUsername(String username); // 컬랙션

 Member findMemberByUsername(String username); // 단건

 Optional<Member> findOptionalByUsername(String username); // 단건
``` 

``` java
    @Test
    public void returnType() throws Exception {
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("BBB", 20);

        memberRepository.save(m1);
        memberRepository.save(m2);

        List<Member> result = memberRepository.findListByUsername("AAAASD");
        System.out.println("result = " + result); // []

        Member findMember = memberRepository.findMemberByUsername("Asdsad");
        System.out.println("findMember = " + findMember); // null

        // 단건일 경우 값이 없을때 순수 JPA에서는 javax.persistence.NoResultException 나오는데
        // Spring Data JPA 경우에는 Null을 반환함.

        Optional<Member> OptionalMember = memberRepository.findOptionalByUsername("AABBA");
        System.out.println("OptionalMember = " + OptionalMember);

        // Java8 이상부터는 값이 있을 수도 있고 없는 경우는 Optional 쓰면 됨.
    }
``` 
스프링 데이터 JPA 공식 문서: https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repository-query-return-types

**조회 결과가 많거나 없으면?**
- 컬렉션
  - 결과 없음: 빈 컬렉션 반환
- 단건 조회
  - 결과 없음: null 반환
  - 결과가 2건 이상: javax.persistence.NonUniqueResultException 예외 발생 

> 참고: 단건으로 지정한 메서드를 호출하면 스프링 데이터 JPA는 내부에서 JPQL의 Query.getSingleResult() 메서드를 호출한다.  
> 이 메서드를 호출했을 때 조회 결과가 없으면 javax.persistence.NoResultException 예외가 발생하는데 개발자 입장에서   
> 다루기가 상당히 불편하다. 스프링 데이터 JPA는 단건을 조회할 때 이 예외가 발생하면 예외를 무시하고 대신에 null 을 반환한다.

### 순수 JPA 페이징과 정렬

- JPA 페이징 리포지토리 코드

#### MemberJpaRepository - 추가
``` java
  public List<Member> findByPage(int age, int offset, int limit) {
      return em.createQuery(
                "select m from Member m where m.age = :age order by m.username desc", Member.class)
                .setParameter("age", age)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
  }

  public long totalCont(int age) {
      return em.createQuery(
                "select count(m) from Member m where m.age = :age", Long.class)
                .setParameter("age", age)
                .getSingleResult();
  }
``` 

### 스프링 데이터 JPA 페이징과 정렬

**페이징과 정렬 파라미터**
- org.springframework.data.domain.Sort : 정렬 기능
- org.springframework.data.domain.Pageable : 페이징 기능 (내부에 Sort 포함)

**특별한 반환 타입**
- org.springframework.data.domain.Page : 추가 count 쿼리 결과를 포함하는 페이징
- org.springframework.data.domain.Slice : 추가 count 쿼리 없이 다음 페이지만 확인 가능(내부적 으로 limit + 1조회)
- List (자바 컬렉션): 추가 count 쿼리 없이 결과만 반환

``` java
 Page<Member> findByAge(int age, Pageable pageable); // count 쿼리 사용
 
 Slice<Member> findSliceByAge(int age, Pageable pageable); // count 쿼리 없음
 
 List<Member> findListByAge(int age, Pageable pageable); // count 쿼리 없음

``` 

``` java
 @Query(value = "select m from Member m left join m.team t", countQuery = "select count(m.username) from Member m")
 Page<Member> findQueryByAge(int age, Pageable pageable);  // count 쿼리 분리 조인 없이 맴버만 카운팅해서 성능적으로 좋음
``` 

- 두 번째 파라미터로 받은 Pageable 은 인터페이스다. 따라서 실제 사용할 때는 해당 인터페이스를 구현한 org.springframework.data.domain.PageRequest 객체를 사용한다.
- PageRequest 생성자의 첫 번째 파라미터에는 현재 페이지를, 두 번째 파라미터에는 조회할 데이터 수를 입력한다. 여기에 추가로 정렬 정보도 파라미터로 사용할 수 있다. 참고로 페이지는 0부터 시작한다.

> 주의: Page는 1부터 시작이 아니라 0부터 시작이다.

#### Page 인터페이스
```java
public interface Page<T> extends Slice<T> {
    int getTotalPages(); //전체 페이지 수
    long getTotalElements(); //전체 데이터 수
    <U> Page<U> map(Function<? super T, ? extends U> converter); //변환기
}
``` 

#### Slice 인터페이스
```java
public interface Slice<T> extends Streamable<T> {
    int getNumber();             //현재 페이지
    int getSize();               //페이지 크기
    int getNumberOfElements();   //현재 페이지에 나올 데이터 수
    List<T> getContent();        //조회된 데이터
    boolean hasContent();        //조화된 데이터 존재 여부
    Sort getSort();              //정렬 정보
    boolean isFirst();           //현재 페이자가 첫 페이지 인지 여부
    boolean isLast();            //현재 페이지가 마지막 페이지 인지 여부
    boolean hasNext();           //다음 페이지 여부
    boolean hasPrevious();       //이전 페이지 여부
    Pageable getPageable();      //페이지 요청 정보
    Pageable nextPageable();     //다음 페이지 객체
    Pageable previousPageable(); //이전 페이지 객체
    <U> Slice<U> map(Function<? super T, ? extends U> converter); //변환기
}

```

- 페이지를 유지하면서 엔티티를 DTO로 변환하기

``` java
 Page<Member> page = memberRepository.findByAge(age, pageRequest);
 
 Page<MemberDto> toMap = page.map(member -> new MemberDto(member.getId(), member.getUsername(), null));
```

### 벌크성 수정 쿼리

#### JPA를 사용한 벌크성 수정 쿼리
``` java
 public int bulkAgePlus(int age) {
     int resultCount = em.createQuery(
                "update Member m set m.age = m.age + 1" +
                        "where m.age >= :age")
                .setParameter("age", age)
                .executeUpdate();
     return resultCount;
 }
``` 

#### 스프링 데이터 JPA를 사용한 벌크성 수정 쿼리  
***벌크성 쿼리란 DB에서 여러개의 레코드를 한번에 추가/수정/삭제하는 쿼리를 말한다.***

``` java
 @Modifying
 @Query("update Member m set m.age = m.age + 1 where m.age >= :age")
 int bulkAgePlus(@Param("age") int age);
```

- 벌크성 수정, 삭제 쿼리는 **@Modifying** 어노테이션을 사용
  - 사용하지 않으면 예외 발생
- 벌크성 쿼리를 실행하고 나서 영속성 컨텍스트 초기화: @Modifying(clearAutomatically = true)
- 벌크성 수정 후 데이터 데이터 조회시 초기화 해주는게 좋음.


``` java
 @Modifying(clearAutomatically = true)
 @Query(value = "update Member m set m.age = m.age + 1 where m.age >= :age")
 int bulkAgePlus(@Param("age") int age);
```

#### em.clear() 를 Spring data JPA에서 @Modifying(clearAutomatically = true) 이런식으로 사용 가능 (기본 값은 false)
``` java
 @PersistenceContext
 EntityManager em;
 
 em.clear();
```

### @EntityGraph

- @EntityGraph
  - 연관된 엔티티들을 SQL 한번으로 조회하는 방법이다.
  - Spring Data JPA에서 fetch join 하는 것을 말함.

***member -> team이 지연로딩 관계로 엮여있다면, team 조회 시 할 때 마다 쿼리가 실행된다. N + 1 문제 발생***
``` java
@ManyToOne(fetch = LAZY)
@JoinColumn(name = "team_id")
private Team team;
``` 


#### JPQL 페치 조인 - N + 1 문제 해결함.
``` java
@Query(value = "select m from Member m left join fetch m.team")
List<Member> findMemberFetchJoin();
``` 

스프링 데이터 JPA는 JPA가 제공하는 엔티티 그래프 기능을 편리하게 사용하게 도와준다.  
이 기능을 사용 하면 JPQL 없이 페치 조인을 사용할 수 있다. (JPQL + 엔티티 그래프도 가능)

#### EntityGraph
``` java
//공통 메서드 오버라이드
@Override
@EntityGraph(attributePaths = {"team"}) 
List<Member> findAll();

//JPQL + 엔티티 그래프 
@EntityGraph(attributePaths = {"team"}) 
@Query("select m from Member m") 
List<Member> findMemberEntityGraph();

//메서드 이름으로 쿼리에서 특히 편리하다. 
@EntityGraph(attributePaths = {"team"}) 
List<Member> findByUsername(String username);
```

#### EntityGraph 정리
- 사실상 페치 조인(FETCH JOIN)의 간편 버전
- LEFT OUTER JOIN 사용

## JPA Hint & Lock

### JPA Hint  
***JPA 쿼리 힌트(SQL 힌트가 아니라 JPA 구현체에게 제공하는 힌트)***

#### 쿼리 힌트 사용
``` java
@QueryHints(value = @QueryHint(name = "org.hibernate.readOnly", value ="true"))
Member findReadOnlyByUsername(String username);
``` 

``` java
@Test
public void queryHint() throws Exception {
    
    //given
    memberRepository.save(new Member("member1", 10));
    em.flush();
    em.clear();

    //when
    Member member = memberRepository.findReadOnlyByUsername("member1");
    member.setUsername("member2");
    
    em.flush(); //Update Query 실행X 
}
``` 

### Lock

``` java
@Lock(LockModeType.PESSIMISTIC_WRITE)
List<Member> findLockByUsername(String username);
```

### 사용자 정의 리포지토리 구현

- 스프링 데이터 JPA 리포지토리는 인터페이스만 정의하고 구현체는 스프링이 자동 생성
- 스프링 데이터 JPA가 제공하는 인터페이스를 직접 구현하면 구현해야 하는 기능이 너무 많음
- 다양한 이유로 인터페이스의 메서드를 직접 구현하고 싶다면?
  - JPA 직접 사용( EntityManager ) 
  - 스프링 JDBC Template 사용 
  - MyBatis 사용
  - 데이터베이스 커넥션 직접 사용 등등... 
  - Querydsl 사용

#### 사용자 정의 인터페이스
``` java
public interface MemberRepositoryCustom {
    List<Member> findMemberCustom();
}
``` 

#### 사용자 정의 인터페이스 구현 클래스
``` java
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom {
    
    private final EntityManager em;
    
    @Override
    public List<Member> findMemberCustom() {
         return em.createQuery("select m from Member m").getResultList();
         
   } 
}

``` 

#### 사용자 정의 인터페이스 구현 클래스
``` java
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

}
``` 

#### 사용자 정의 메서드 호출 코드
``` java
List<Member> result = memberRepository.findMemberCustom();
``` 

- 사용자 정의 구현 클래스
  - 규칙: 리포지토리 인터페이스 이름 + Impl
  - 스프링 데이터 JPA가 인식해서 스프링 빈으로 등록

> 참고: 실무에서는 주로 QueryDSL이나 SpringJdbcTemplate을 함께 사용할 때 사용자 정의 리포지토 리 기능 자주 사용한다.

> 참고: 항상 사용자 정의 리포지토리가 필요한 것은 아니다. 그냥 임의의 리포지토리를 만들어도 된다.  
> 예를 들어 MemberQueryRepository를 인터페이스가 아닌 클래스로 만들고 스프링 빈으로 등록해서  
> 직접 사용해도 된다. 물론 이 경우 스프링 데이터 JPA와는 아무런 관계 없이 별도로 동작한다.

### Auditing

***컬럼에 등록일, 수정일, 등록자, 수정자 남기고 싶을때. 관리해주는 기능***

#### 순수 JPA 사용 (등록일, 수정일)
``` java
@MappedSuperclass
@Getter @Setter
public class JpaBaseEntity {

    @Column(updatable = false)
    private LocalDateTime createdDate;

    private LocalDateTime updatedDate;

    @PrePersist // save 저장하기 전에 이벤트 발생
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdDate = now;
        updatedDate = now;
    }

    @PreUpdate // flush 업데이트 전에 이벤트 발생
    public void preUpdate() {
        updatedDate = LocalDateTime.now();
    }

}
``` 

#### JPA 주요 이벤트 어노테이션
- @PrePersist, @PostPersist 
- @PreUpdate, @PostUpdate

### 스프링 데이터 JPA 사용

#### 설정
- @EnableJpaAuditing -> 스프링 부트 설정 클래스에 설정
- @EntityListeners(AuditingEntityListener.class) -> 엔티티에 설정

#### 사용 어노테이션
- @CreatedDate
- @LastModifiedDate
- @CreatedBy
- @LastModifiedBy

#### 스프링 데이터 Auditing 적용
``` java
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
public class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @CreatedBy
    @Column(updatable = false)
    private String createBy;

    @LastModifiedBy
    private String lastModifiedBy;
}
``` 

#### 등록자, 수정자를 처리해주는 AuditorAware 스프링 빈 등록
``` java
@EnableJpaAuditing // @EnableJpaAuditing도 함께 등록해야 한다.
@SpringBootApplication
public class DataJpaApplication {
         
    public static void main(String[] args) {
              SpringApplication.run(DataJpaApplication.class, args);
    }
    
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of(UUID.randomUUID().toString());
    }
}
``` 

#### 실무에서는 세션 정보나, 스프링 시큐리티 로그인 정보에서 ID를 받음

> 참고: 실무에서 대부분의 엔티티는 등록시간, 수정시간이 필요하지만, 등록자, 수정자는 없을 수도 있다.   
> 그래서 다음과 같이 Base 타입을 분리하고, 원하는 타입을 선택해서 상속한다.

``` java
public class BaseTimeEntity {
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;
   
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
}
    
public class BaseEntity extends BaseTimeEntity {
    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String lastModifiedBy;
}
``` 
 
#### 시간만 필요하면 BaseTimeEntity 상속받고, 4가지 다 필요하면 BaseEntity 상속 받으면 됨.

## Web 확장 - 페이징과 정렬

***스프링 데이터가 제공하는 페이징과 정렬 기능을 스프링 MVC에서 편리하게 사용할 수 있다.***

#### 페이징과 정렬 예제
``` java
@GetMapping("/members")
public Page<Member> list(Pageable pageable) {
     Page<Member> page = memberRepository.findAll(pageable);
     return page;
}
``` 

- 파라미터로 Pageable 을 받을 수 있다.
- Pageable 은 인터페이스, 실제는 org.springframework.data.domain.PageRequest 객체 생성

#### 요청 파라미터
- 예) /members?page=0&size=3&sort=id,desc&sort=username,desc
- page: 현재 페이지, 0부터 시작한다.
- size: 한 페이지에 노출할 데이터 건수
- sort: 정렬 조건을 정의한다.  
  예) 정렬 속성,정렬 속성...(ASC | DESC), 정렬 방향을 변경하고 싶으면 sort 파라미터 추가 ( asc 생략 가능)


#### 페이징 설정
- 글로벌 설정: application.yml

``` yml
  data:
    web:
      pageable:
        default-page-size: 5
        max-page-size: 2000
``` 

### 개별 설정: @PageableDefault

``` java
@GetMapping("/members")
public Page<Member> list(@PageableDefault(size = 10, sort = "username") Pageable pageable) {
     Page<Member> page = memberRepository.findAll(pageable);
     return page;
}
``` 

#### Page 내용을 DTO로 변환하기
- 엔티티를 API로 노출하면 다양한 문제가 발생한다. 그래서 엔티티를 꼭 DTO로 변환해서 반환해야 한다.
- Page는 map() 을 지원해서 내부 데이터를 다른 것으로 변경할 수 있다.


#### MemberDto
``` java
@Data
public class MemberDto {

    private Long id;
    private String username;
    private String teamName;

    public MemberDto(Long id, String username, String teamName) {
        this.id = id;
        this.username = username;
        this.teamName = teamName;
    }

    public MemberDto(Member member) {
        this.id = member.getId();
        this.username = member.getUsername();
    }
}

``` 

#### Page.map() 사용
``` java
@GetMapping("/members")
public Page<MemberDto> list(@PageableDefault(size = 10, sort = "username") Pageable pageable) {
     Page<Member> page = memberRepository.findAll(pageable);
     Page<MemberDto> map = page.map(member -> new MemberDto(member));

     return map;
}
``` 

#### Page.map() 코드 최적화
``` java
@GetMapping("/members")
public Page<MemberDto> list(@PageableDefault(size = 10, sort = "username") Pageable pageable) {
    return memberRepository.findAll(pageable).map(MemberDto::new);
}
``` 

### Page를 1부터 시작하기
- 스프링 데이터는 Page를 0부터 시작한다.
- 만약 1부터 시작하려면?
  - Pageable, Page를 파리미터와 응답 값으로 사용히지 않고, 직접 클래스를 만들어서 처리한다.  
    그리고 직접 PageRequest(Pageable 구현체)를 생성해서 리포지토리에 넘긴다. 
    응답값도 Page 대신에 직접 만들어서 제공해야 한다.
  - spring.data.web.pageable.one-indexed-parameters 를 true 로 설정한다. 근데 한계가 있다.
