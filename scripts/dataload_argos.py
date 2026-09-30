import os
import random

import psycopg2

# =========================================================
# DATALOAD ARGOS
# Gera mais de 500 registros de teste
#
# Execute pelo Docker Compose: docker compose --profile dataload run --build --rm dataload
# =========================================================

random.seed(42)

# =========================================================
# CONEXAO COM O POSTGRES
# =========================================================

conexao = psycopg2.connect(
    host=os.environ.get("DATALOAD_DB_HOST", "postgres"),
    database=os.environ["POSTGRES_DB"],
    user=os.environ["DATASOURCE_USER"],
    password=os.environ["DATASOURCE_PASSWORD"],
    port=os.environ.get("DATALOAD_DB_PORT", "5432")
)

cursor = conexao.cursor()

try:
    cursor.execute("SELECT email FROM app_user WHERE email LIKE %s LIMIT 1", ("%@argos.test",))
    usuario_existente = cursor.fetchone()
    if usuario_existente is not None:
        raise Exception(
            "A carga de teste já existe (" + usuario_existente[0] + "). "
            "Use um banco local limpo para executá-la novamente."
        )

    # =====================================================
    # 1. PEGAR A PERMISSAO PECUARISTA
    # =====================================================
    cursor.execute("""
        SELECT id
        FROM permission
        WHERE code = 'PECUARISTA'
    """)

    resultado = cursor.fetchone()

    if resultado is None:
        raise Exception("Permissao PECUARISTA nao encontrada.")

    permission_id = resultado[0]

    # =====================================================
    # 2. CRIAR 11 USUARIOS
    # =====================================================
    nomes = [
        "Letícia Favoretto",
        "Enzo Neves",
        "Luiz Henrique",
        "Beatriz Dias",
        "Vinicius de Oliveira",
        "Matheus Fazan",
        "Eduardo Toledo",
        "Murilo Paiva",
        "Lucca Cansani",
        "Pietra Gomes",
        "Leticia Ribeiro"
    ]

    emails = [
        "leticia.favoretto@argos.test",
        "enzo.neves@argos.test",
        "luiz.henrique@argos.test",
        "beatriz.dias@argos.test",
        "vinicius.oliveira@argos.test",
        "matheus.fazan@argos.test",
        "eduardo.toledo@argos.test",
        "murilo.paiva@argos.test",
        "lucca.cansani@argos.test",
        "pietra.gomes@argos.test",
        "leticia.ribeiro@argos.test"
    ]

    usuarios = []

    for i in range(11):
        cpf = "9" + str(i + 1).zfill(10)

        cursor.execute("""
            INSERT INTO app_user (
                permission_id,
                name,
                cpf,
                email,
                first_login,
                active
            )
            VALUES (%s, %s, %s, %s, false, true)
            RETURNING id
        """, (
            permission_id,
            nomes[i],
            cpf,
            emails[i]
        ))

        usuario_id = cursor.fetchone()[0]
        usuarios.append(usuario_id)

    print("11 usuarios criados")

    # =====================================================
    # 3. CRIAR 11 PROPRIEDADES
    # =====================================================
    propriedades = []

    cidades = [
        "Sao Paulo",
        "Barretos",
        "Ribeirao Preto",
        "Bauru",
        "Aracatuba"
    ]

    for i in range(11):
        cursor.execute("""
            INSERT INTO property (
                user_id,
                name,
                address,
                city,
                state,
                area_hectares,
                active
            )
            VALUES (%s, %s, %s, %s, %s, %s, true)
            RETURNING id
        """, (
            usuarios[i],
            "Fazenda " + str(i + 1),
            "Endereco de teste " + str(i + 1),
            random.choice(cidades),
            "SP",
            random.randint(100, 1000)
        ))

        propriedade_id = cursor.fetchone()[0]
        propriedades.append(propriedade_id)

        cursor.execute("""
            INSERT INTO user_property (
                user_id,
                property_id
            )
            VALUES (%s, %s)
        """, (
            usuarios[i],
            propriedade_id
        ))

    print("11 propriedades criadas")

    # =====================================================
    # 4. CRIAR 40 REBANHOS
    # =====================================================
    rebanhos = []

    racas = [
        "NELORE",
        "ANGUS",
        "BRAHMAN",
        "HEREFORD",
        "SENEPOL"
    ]

    for i in range(40):
        propriedade = random.choice(propriedades)

        cursor.execute("""
            INSERT INTO herd (
                property_id,
                herd_name,
                breed,
                head_count,
                active
            )
            VALUES (%s, %s, %s, %s, true)
            RETURNING id
        """, (
            propriedade,
            "Rebanho " + str(i + 1),
            random.choice(racas),
            random.randint(80, 250)
        ))

        rebanho_id = cursor.fetchone()[0]
        rebanhos.append(rebanho_id)

    print("40 rebanhos criados")

    # =====================================================
    # 5. CRIAR 150 LOTES
    # =====================================================
    lotes = []

    categorias_lote = [
        "ENGORDA",
        "CRIA",
        "RECRIA"
    ]

    for i in range(150):
        rebanho = random.choice(rebanhos)

        cursor.execute("""
            INSERT INTO lot (
                herd_id,
                lot_code,
                category,
                head_count,
                active
            )
            VALUES (%s, %s, %s, %s, true)
            RETURNING id
        """, (
            rebanho,
            "LOTE-" + str(i + 1),
            random.choice(categorias_lote),
            random.randint(10, 80)
        ))

        lote_id = cursor.fetchone()[0]
        lotes.append(lote_id)

    print("150 lotes criados")

    # =====================================================
    # 6. CRIAR 10 FORNECEDORES
    # =====================================================
    fornecedores = []

    for i in range(1, 11):
        cnpj = str(80000000000000 + i)

        cursor.execute("""
            INSERT INTO supplier (
                company_name,
                cnpj,
                email,
                phone,
                address,
                active
            )
            VALUES (%s, %s, %s, %s, %s, true)
            RETURNING id
        """, (
            "Fornecedor Veterinario " + str(i),
            cnpj,
            "fornecedor" + str(i) + "@argos.test",
            "1199999" + str(i).zfill(4),
            "Endereco fornecedor " + str(i)
        ))

        fornecedor_id = cursor.fetchone()[0]
        fornecedores.append(fornecedor_id)

    print("10 fornecedores criados")

    # =====================================================
    # 7. PEGAR CATEGORIA DE MEDICAMENTO
    # =====================================================
    cursor.execute("""
        SELECT id
        FROM therapeutic_category
        WHERE name = 'Antibiótico'
        LIMIT 1
    """)

    resultado = cursor.fetchone()

    if resultado is None:
        raise Exception("Categoria Antibiótico nao encontrada.")

    categoria_medicamento = resultado[0]

    # =====================================================
    # 8. CRIAR 20 MEDICAMENTOS
    # =====================================================
    medicamentos = []

    for i in range(1, 21):
        cursor.execute("""
            INSERT INTO medicine (
                supplier_id,
                therapeutic_category_id,
                brand_name,
                active_ingredient,
                indication,
                description,
                needs_prescription,
                meat_withdrawal_days,
                dose,
                unit,
                route,
                active
            )
            VALUES (
                %s, %s, %s, %s, %s, %s,
                %s, %s, %s, %s, %s, true
            )
            RETURNING id
        """, (
            random.choice(fornecedores),
            categoria_medicamento,
            "Medicamento " + str(i),
            "Principio Ativo " + str(i),
            "Tratamento veterinario",
            "Medicamento criado pelo DataLoad",
            random.choice([True, False]),
            random.choice([7, 14, 21, 28]),
            random.randint(1, 5),
            "ml",
            random.choice([
                "INTRAMUSCULAR",
                "SUBCUTANEA",
                "ORAL"
            ])
        ))

        medicamento_id = cursor.fetchone()[0]
        medicamentos.append(medicamento_id)

    print("20 medicamentos criados")

    # =====================================================
    # 9. CRIAR 40 LOTES DE MEDICAMENTO
    # =====================================================
    lotes_medicamento = []

    for i in range(1, 41):
        medicamento = medicamentos[(i - 1) % len(medicamentos)]

        cursor.execute("""
            INSERT INTO medicine_batch (
                medicine_id,
                batch_number,
                manufacture_date,
                expiry_date,
                delivery_date,
                initial_quantity,
                min_stock,
                max_stock,
                storage_place,
                status
            )
            VALUES (
                %s,
                %s,
                current_date - 100,
                current_date + 300,
                current_date - 30,
                5000,
                500,
                7000,
                %s,
                'DISPONIVEL'
            )
            RETURNING id
        """, (
            medicamento,
            "BATCH-" + str(i),
            "Deposito " + str((i % 4) + 1)
        ))

        batch_id = cursor.fetchone()[0]
        lotes_medicamento.append(batch_id)

    print("40 lotes de medicamento criados")

    # =====================================================
    # 10. PEGAR O USUARIO TECNICO DA RPA
    # =====================================================
    cursor.execute("""
        SELECT id
        FROM app_user
        WHERE cpf = '00000000000'
    """)

    resultado = cursor.fetchone()

    if resultado is None:
        raise Exception("Usuario tecnico RPA nao encontrado.")

    usuario_rpa = resultado[0]

    # =====================================================
    # 11. CRIAR ENTRADA DE ESTOQUE PARA CADA BATCH
    # =====================================================
    for batch in lotes_medicamento:
        cursor.execute("""
            INSERT INTO stock_movement (
                medicine_batch_id,
                user_id,
                type,
                quantity,
                reason
            )
            VALUES (%s, %s, 'E', 5000, 'CARGA_INICIAL')
        """, (
            batch,
            usuario_rpa
        ))

    print("40 entradas de estoque criadas")

    # =====================================================
    # 12. CRIAR 200 APLICACOES EM LOTES
    # =====================================================
    for i in range(200):
        lote = random.choice(lotes)
        batch = random.choice(lotes_medicamento)
        usuario = random.choice(usuarios)

        cursor.execute("""
            INSERT INTO application (
                lot_id,
                medicine_batch_id,
                user_id,
                application_date,
                application_time,
                dose,
                unit,
                route,
                head_count_applied,
                goal,
                notes,
                active
            )
            VALUES (
                %s,
                %s,
                %s,
                current_date,
                current_time,
                5,
                'ml',
                'INTRAMUSCULAR',
                1,
                %s,
                %s,
                true
            )
        """, (
            lote,
            batch,
            usuario,
            "Aplicacao DataLoad " + str(i + 1),
            "Registro criado automaticamente para teste"
        ))

    print("200 aplicacoes criadas")

    # =====================================================
    # SALVAR TODAS AS ALTERACOES
    # =====================================================
    conexao.commit()

    print()
    print("==========================================")
    print("DATALOAD CONCLUIDO COM SUCESSO")
    print("==========================================")
    print()

    # =====================================================
    # CONTAGEM DOS REGISTROS
    # =====================================================
    tabelas = [
        "app_user",
        "property",
        "user_property",
        "herd",
        "lot",
        "supplier",
        "medicine",
        "medicine_batch",
        "application",
        "stock_movement"
    ]

    total = 0

    for tabela in tabelas:
        cursor.execute("SELECT COUNT(*) FROM " + tabela)
        quantidade = cursor.fetchone()[0]

        print(tabela + ": " + str(quantidade))
        total += quantidade

    print()
    print("Total contado nessas tabelas:", total)

except Exception as erro:
    conexao.rollback()

    print()
    print("==========================================")
    print("ERRO NO DATALOAD")
    print("==========================================")
    print(erro)
    raise

finally:
    cursor.close()
    conexao.close()

