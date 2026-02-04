<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Header -->
    <nav class="bg-white shadow">
      <div class="max-w-6xl mx-auto px-4 py-4">
        <div class="flex justify-between items-center">
          <h1 class="text-2xl font-bold text-blue-600">Intuitive Care</h1>
          <div class="flex gap-4">
            <button @click="view = 'dashboard'" :class="view === 'dashboard' ? 'bg-blue-600 text-white' : 'bg-gray-200'" class="px-4 py-2 rounded">
              Dashboard
            </button>
            <button @click="view = 'operadoras'" :class="view === 'operadoras' ? 'bg-blue-600 text-white' : 'bg-gray-200'" class="px-4 py-2 rounded">
              Operadoras
            </button>
          </div>
        </div>
      </div>
    </nav>

    <main class="max-w-6xl mx-auto px-4 py-8">
      <!-- Dashboard -->
      <div v-if="view === 'dashboard'" class="space-y-6">
        <!-- Cards -->
        <div class="grid grid-cols-3 gap-4">
          <div class="bg-white p-6 rounded shadow">
            <h3 class="text-gray-600 text-sm">Total Despesas</h3>
            <p class="text-2xl font-bold mt-2">{{ formatarMoeda(stats.total_despesas) }}</p>
          </div>
          <div class="bg-white p-6 rounded shadow">
            <h3 class="text-gray-600 text-sm">Média</h3>
            <p class="text-2xl font-bold mt-2">{{ formatarMoeda(stats.media_despesas) }}</p>
          </div>
          <div class="bg-white p-6 rounded shadow">
            <h3 class="text-gray-600 text-sm">Mediana</h3>
            <p class="text-2xl font-bold mt-2">{{ formatarMoeda(stats.mediana_despesas) }}</p>
          </div>
        </div>

        <!-- Gráfico UF -->
        <div class="bg-white p-6 rounded shadow">
          <h2 class="text-lg font-bold mb-4">Distribuição por UF</h2>
          <div style="position: relative; height: 300px;">
            <canvas id="graficoUF"></canvas>
          </div>
        </div>

        <!-- Top 5 -->
        <div class="bg-white p-6 rounded shadow">
          <h2 class="text-lg font-bold mb-4">Top 5 Operadoras</h2>
          <table class="w-full">
            <thead>
              <tr class="border-b">
                <th class="text-left py-2">Nome</th>
                <th class="text-left py-2">UF</th>
                <th class="text-right py-2">Total</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="op in stats.top_5_operadoras" :key="op.id_operadora" class="border-b hover:bg-gray-50">
                <td class="py-2">{{ op.nome }}</td>
                <td>{{ op.uf }}</td>
                <td class="text-right">{{ formatarMoeda(op.total_despesas) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Operadoras -->
      <div v-if="view === 'operadoras'" class="space-y-4">
        <div class="flex gap-2">
          <input 
            v-model="search"
            placeholder="Buscar operadora..." 
            class="flex-1 px-4 py-2 border rounded"
          />
          <button @click="carregarOperadoras()" class="bg-blue-600 text-white px-4 py-2 rounded">
            Carregar
          </button>
        </div>

        <div v-if="loading" class="text-center py-8">Carregando...</div>
        <div v-else-if="erro" class="bg-red-100 text-red-700 p-4 rounded">{{ erro }}</div>
        <div v-else class="space-y-4">
          <table class="w-full bg-white rounded shadow overflow-hidden">
            <thead>
              <tr class="bg-gray-100 border-b">
                <th class="text-left p-3">Nome</th>
                <th class="text-left p-3">CNPJ</th>
                <th class="text-left p-3">UF</th>
                <th class="text-left p-3">Tipo</th>
                <th class="text-center p-3">Ação</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="op in operadoras" :key="op.id_operadora" class="border-b hover:bg-gray-50">
                <td class="p-3">{{ op.nome }}</td>
                <td class="p-3">{{ op.cnpj }}</td>
                <td class="p-3">{{ op.uf }}</td>
                <td class="p-3">{{ op.tipo }}</td>
                <td class="text-center p-3">
                  <button @click="verDetalhes(op)" class="text-blue-600">Ver</button>
                </td>
              </tr>
            </tbody>
          </table>

          <!-- Paginação -->
          <div class="flex justify-center gap-2">
            <button @click="pagina--" :disabled="pagina === 1" class="px-3 py-1 border rounded disabled:opacity-50">←</button>
            <span class="px-3 py-1">Página {{ pagina }}</span>
            <button @click="pagina++" class="px-3 py-1 border rounded">→</button>
          </div>
        </div>
      </div>

      <!-- Modal -->
      <div v-if="modalAberto" class="fixed inset-0 bg-black/50 flex items-center justify-center">
        <div class="bg-white p-6 rounded max-w-md w-full">
          <h2 class="text-lg font-bold mb-4">{{ operadoraSelecionada.nome }}</h2>
          <p><strong>CNPJ:</strong> {{ operadoraSelecionada.cnpj }}</p>
          <p><strong>UF:</strong> {{ operadoraSelecionada.uf }}</p>
          <p><strong>Tipo:</strong> {{ operadoraSelecionada.tipo }}</p>
          <button @click="modalAberto = false" class="mt-4 bg-blue-600 text-white px-4 py-2 rounded w-full">
            Fechar
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import Chart from 'chart.js/auto'

const view = ref('dashboard')
let chartInstance = null
const operadoras = ref([])
const search = ref('')
const pagina = ref(1)
const limit = ref(10)
const loading = ref(false)
const erro = ref(null)
const stats = ref({
  total_despesas: 0,
  media_despesas: 0,
  mediana_despesas: 0,
  top_5_operadoras: []
})
const modalAberto = ref(false)
const operadoraSelecionada = ref({})

const API = 'http://localhost:8000/api'

// Formatar moeda
const formatarMoeda = (valor) => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  }).format(valor)
}

// Carregar operadoras
const carregarOperadoras = async () => {
  loading.value = true
  erro.value = null
  try {
    const params = new URLSearchParams({
      page: pagina.value,
      limit: limit.value,
      search: search.value
    })
    const res = await fetch(`${API}/operadoras?${params}`)
    const data = await res.json()
    operadoras.value = data.data
  } catch (e) {
    erro.value = 'Erro ao carregar operadoras'
  } finally {
    loading.value = false
  }
}

// Renderizar gráfico
const renderizarGrafico = async () => {
  await nextTick()
  const canvas = document.getElementById('graficoUF')
  if (!canvas) return
  
  if (chartInstance) {
    chartInstance.destroy()
  }
  
  const ufs = Object.keys(stats.value.distribuicao_por_uf)
  const valores = Object.values(stats.value.distribuicao_por_uf)
  
  chartInstance = new Chart(canvas, {
    type: 'bar',
    data: {
      labels: ufs,
      datasets: [{
        label: 'Despesas por UF',
        data: valores,
        backgroundColor: 'rgba(59, 130, 246, 0.6)',
        borderColor: 'rgb(59, 130, 246)',
        borderWidth: 1
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        y: {
          beginAtZero: true
        }
      }
    }
  })
}

// Carregar estatísticas
const carregarStats = async () => {
  try {
    const res = await fetch(`${API}/estatisticas`)
    stats.value = await res.json()
    renderizarGrafico()
  } catch (e) {
    erro.value = 'Erro ao carregar estatísticas'
  }
}

// Ver detalhes
const verDetalhes = (op) => {
  operadoraSelecionada.value = op
  modalAberto.value = true
}

// Carregar dados ao montar
onMounted(() => {
  carregarOperadoras()
  carregarStats()
})
</script>

<style scoped>
table {
  width: 100%;
  border-collapse: collapse;
}
</style>
