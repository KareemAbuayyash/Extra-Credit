package com.example.payroll.employeeService;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.payroll.departmentService.Department;
import com.example.payroll.departmentService.DepartmentRepository;
import com.example.payroll.exceptions.ResourceNotFoundException;
import com.example.payroll.security.User;
import com.example.payroll.security.UserRepository;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    @Autowired
    private EmployeeRepository repository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private EmployeeModelAssembler assembler;
    @Autowired
    private UserRepository userRepository;

    @Override
    public CollectionModel<EntityModel<EmployeeDTO>> findAll() {
        List<EntityModel<EmployeeDTO>> employees = repository.findAll().stream() //
                .map(EmployeeMapper::toDTO) //
                .map(assembler::toModel) //
                .collect(Collectors.toList());

        return CollectionModel.of(employees, linkTo(methodOn(EmployeeController.class).all()).withSelfRel());

    }

    @Override
public ResponseEntity<?> newEmployee(EmployeeDTO newEmployee) {
    Department dep = departmentRepository.findByName(newEmployee.getDepartmentName())
            .orElseThrow(() -> new ResourceNotFoundException("Department with Name "+ newEmployee.getDepartmentName() + " not found."));

    // ✅ Check if user already exists
    User user = userRepository.findByUsername(newEmployee.getUsername()).orElse(null);

    if (user == null) {
        user = new User();
        user.setUsername(newEmployee.getUsername());
        user.setPassword(new BCryptPasswordEncoder().encode(newEmployee.getPassword()));
        user.setRole("ROLE_" + newEmployee.getRole()); // e.g., ROLE_ADMIN
        user = userRepository.save(user);
    }

    // ✅ Now assign the user to the employee
    Employee employee = EmployeeMapper.toEntity(newEmployee, dep);
    employee.setUser(user);

    employee = repository.save(employee);

    EntityModel<EmployeeDTO> entityModel = assembler.toModel(EmployeeMapper.toDTO(employee));

    return ResponseEntity
            .created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri())
            .body(entityModel);
}


    @Override
    public ResponseEntity<?> findById(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName(); // Get username directly

        User authenticatedUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Employee employee = repository.findById(id) //
                .orElseThrow(() -> new ResourceNotFoundException("Employee with ID " + id + " not found."));

        if (!authenticatedUser.getRole().equals("ROLE_ADMIN")
                && !authenticatedUser.getId().equals(employee.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not allowed to access this data.");
        }

        return ResponseEntity.ok(assembler.toModel(EmployeeMapper.toDTO(employee)));
    }

    @Override
    public EntityModel<EmployeeDTO> findByEmail(String email) {
        Employee employee = repository.findByEmail(email) //
                .orElseThrow(() -> new ResourceNotFoundException("Employee with EMAIL "+email+" not found."));
        return assembler.toModel(EmployeeMapper.toDTO(employee));
    }

    @Override
    public ResponseEntity<?> save(EmployeeDTO newEmployee, Long id) {
        Department dep = departmentRepository.findByName(newEmployee.getDepartmentName())
                .orElseThrow(() -> new ResourceNotFoundException("Department with Name "+ newEmployee.getDepartmentName() + " not found."));

        Employee newEmploye = EmployeeMapper.toEntity(newEmployee, dep);
        Employee updatedEmployee = repository.findById(id) //
                .map(employee -> {
                    employee.setName(newEmployee.getName());
                    employee.setRole(newEmployee.getRole());
                    employee.setEmail(newEmployee.getEmail());
                    employee.setDepartment(dep);
                    return repository.save(employee);
                }) //
                .orElseGet(() -> {
                    return repository.save(newEmploye);
                });

        EntityModel<EmployeeDTO> entityModel = assembler.toModel(EmployeeMapper.toDTO(updatedEmployee));

        return ResponseEntity //
                .created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri()) //
                .body(entityModel);
    }

    @Override
    public ResponseEntity<?> deleteById(Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Override
public ResponseEntity<?> deleteByEmail(String email) {
    Employee employee = repository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("Employee with EMAIL " + email + " not found."));

    repository.delete(employee);
    return ResponseEntity.noContent().build();
}


}
