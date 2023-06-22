

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
